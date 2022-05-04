package com.exoreaction.reactiveservices.service.reactivestreams;

import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket.PublisherWebSocketServlet;
import com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket.SubscriberWebSocketEndpoint;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Contract
public class ReactiveStreams
        implements ContainerLifecycleListener {

    private static final Logger logger = LogManager.getLogger(ReactiveStreams.class);

    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        protected String serviceType() {
            return "reactivestreams";
        }

        @Override
        protected void configure() {
            context.register(ReactiveStreams.class, ReactiveStreams.class, ContainerLifecycleListener.class);
        }
    }

    private ServletContextHandler servletContextHandler;
    private jakarta.inject.Provider<Registry> registryService;
    private WebSocketClient webSocketClient;
    private MessageBodyWorkers messageBodyWorkers;
    private ObjectMapper objectMapper;


    private final Map<ServiceLinkReference, Publisher> publishers = new ConcurrentHashMap<>();

    @Inject
    public ReactiveStreams(ServletContextHandler servletContextHandler,
                           jakarta.inject.Provider<Registry> registryService,
                           WebSocketClient webSocketClient,
                           MessageBodyWorkers messageBodyWorkers,
                           ObjectMapper objectMapper) {
        this.servletContextHandler = servletContextHandler;
        this.registryService = registryService;
        this.webSocketClient = webSocketClient;
        this.messageBodyWorkers = messageBodyWorkers;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onStartup(Container container) {

    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public <T> void publish(ServiceLinkReference streamReference, Publisher<T> publisher, Link websocketLink) {
        publishers.put(streamReference, publisher);

        MessageBodyWriter<Object> writer = null;
        MessageBodyReader<Object> reader = null;

        for (Type genericInterface : publisher.getClass().getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType && ((ParameterizedType)genericInterface).getRawType().equals(Publisher.class))
            {
                ParameterizedType publisherType = ((ParameterizedType)genericInterface);
                Type publisherEventType = publisherType.getActualTypeArguments()[0];
                if (publisherEventType instanceof ParameterizedType && ((ParameterizedType)publisherEventType).getRawType().equals(EventWithResult.class))
                {
                    ParameterizedType eventWithResultType = ((ParameterizedType)publisherEventType);
                    Type eventType = eventWithResultType.getActualTypeArguments()[0];
                    Type resultType = eventWithResultType.getActualTypeArguments()[1];
                    writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>)eventType, eventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (writer == null)
                    {
                        throw new IllegalStateException("Could not find MessageBodyWriter for "+eventType);
                    }

                    reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>)resultType, resultType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (reader == null)
                    {
                        throw new IllegalStateException("Could not find MessageBodyReader for "+resultType);
                    }

                } else {
                    writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>)publisherEventType, publisherEventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (writer == null)
                    {
                        throw new IllegalStateException("Could not find MessageBodyWriter for "+publisherEventType);
                    }
                }
            }
        }

        if (writer == null)
        {
            throw new IllegalStateException("Could not find MessageBodyWriter for "+publisher.getClass());
        }

        String path = URI.create(websocketLink.getHrefAsUriTemplate().createURI()).getPath();
        PublisherWebSocketServlet<T> servlet = new PublisherWebSocketServlet<T>(path, publisher, writer, reader, objectMapper);

        servletContextHandler.addServlet(new ServletHolder(servlet), path);
        logger.info("Published websocket for " + streamReference);
    }

    public <T> void publish(ServiceLinkReference streamReference, Publisher<T> publisher) {

        // Create websocket endpoint
        Link link = registryService.get().getServiceLink(streamReference).orElseThrow(IllegalArgumentException::new);
        publish(streamReference, publisher, link);
    }

    public <T> void subscribe(ServiceLinkReference streamReference,
                              ReactiveEventStreams.Subscriber<T> subscriber) {
        subscribe(streamReference, subscriber, Collections.emptyMap());
    }

    public <T> void subscribe(ServiceLinkReference streamReference,
                              ReactiveEventStreams.Subscriber<T> subscriber,
                              Map<String, String> parameters) {
        Publisher<T> publisher = publishers.get(streamReference);

        if (publisher != null) {
            // Local
            publisher.subscribe(subscriber, parameters);
        } else {
            // Remote
            Link link = registryService.get().getServiceLink(streamReference).orElseThrow(IllegalArgumentException::new);

            subscribe(link, subscriber, parameters, MarkerManager.getMarker(streamReference.toString()));
        }
    }

    public <T> void subscribe(Link link,
                              ReactiveEventStreams.Subscriber<T> subscriber,
                              Map<String, String> parameters,
                              Marker marker) {
        MessageBodyWriter<Object> writer = null;
        MessageBodyReader<Object> reader = null;
        Type eventType = null;

        for (Type genericInterface : subscriber.getClass().getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType && ((ParameterizedType)genericInterface).getRawType().equals(ReactiveEventStreams.Subscriber.class))
            {
                ParameterizedType subscriberType = ((ParameterizedType)genericInterface);
                Type subscriberEventType = subscriberType.getActualTypeArguments()[0];
                if (subscriberEventType instanceof ParameterizedType && ((ParameterizedType)subscriberEventType).getRawType().equals(EventWithResult.class))
                {
                    ParameterizedType eventWithResultType = ((ParameterizedType)subscriberEventType);
                    eventType = eventWithResultType.getActualTypeArguments()[0];
                    Type resultType = eventWithResultType.getActualTypeArguments()[1];
                    reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>)eventType, eventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (reader == null)
                    {
                        throw new IllegalStateException("Could not find MessageBodyReader for "+eventType);
                    }

                    writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>)resultType, resultType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (writer == null)
                    {
                        throw new IllegalStateException("Could not find MessageBodyWriter for "+resultType);
                    }

                } else {
                    eventType = subscriberEventType;
                    reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>)subscriberEventType, subscriberEventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (reader == null)
                    {
                        throw new IllegalStateException("Could not find MessageBodyReader for "+subscriberEventType);
                    }
                }
            }
        }

        if (reader == null)
        {
            throw new IllegalStateException("Could not find MessageBodyReader for "+subscriber.getClass());
        }

        try {
            URI websocketEndpointUri = URI.create(link.getHrefAsUriTemplate().createURI(parameters));
            webSocketClient.connect(new SubscriberWebSocketEndpoint<T>(subscriber, reader, writer, objectMapper, eventType, marker), websocketEndpointUri);
        } catch (IOException e) {
            logger.error("Could not subscribe", e);
        }
    }
}
