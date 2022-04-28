package com.exoreaction.reactiveservices.service.reactivestreams;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket.ReactiveStreamClientWebSocketEndpoint;
import com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket.ReactiveStreamWebSocketServlet;
import com.exoreaction.reactiveservices.service.registry.resources.RegistryService;
import com.lmax.disruptor.EventHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private RegistryService registryService;
    private WebSocketClient webSocketClient;


    private final Map<ServiceLinkReference, PublisherAndSerializer> publishers = new ConcurrentHashMap<>();

    @Inject
    public ReactiveStreams(ServletContextHandler servletContextHandler,
                           RegistryService registryService,
                           WebSocketClient webSocketClient) {
        this.servletContextHandler = servletContextHandler;
        this.registryService = registryService;
        this.webSocketClient = webSocketClient;
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

    public <T> void publish(ServiceLinkReference streamReference, Link websocketLink, Publisher<T> publisher, EventHandler<Event<T>> serializer) {
        publishers.put(streamReference, new PublisherAndSerializer<>(publisher, serializer));

        String path = URI.create(websocketLink.getHrefAsUriTemplate().createURI()).getPath();
        ReactiveStreamWebSocketServlet<T> servlet = new ReactiveStreamWebSocketServlet<>(path, publisher, serializer);
        servletContextHandler.addServlet(new ServletHolder(servlet), path);
        logger.info("Published websocket for "+streamReference);
    }
    public <T> void publish(ServiceLinkReference streamReference, Publisher<T> publisher, EventHandler<Event<T>> serializer) {

        // Create websocket endpoint
        registryService.getServiceLink(streamReference).ifPresentOrElse(link ->
        {
            publish(streamReference, link, publisher, serializer);
        },()->
        {
            logger.warn("Could not find link information for "+streamReference);
        });
    }

    public <T> void subscribe(ServiceLinkReference streamReference,
                              ReactiveEventStreams.Subscriber<T> subscriber) {
        subscribe(streamReference, subscriber, Collections.emptyMap());
    }

    public <T> void subscribe(ServiceLinkReference streamReference,
                              ReactiveEventStreams.Subscriber<T> subscriber,
                              Map<String, String> parameters) {
        PublisherAndSerializer publisherAndSerializer = null; // publishers.get(streamReference);

        if (publisherAndSerializer != null) {
            // Local
            Publisher<T> publisher = (Publisher<T>) publisherAndSerializer.publisher();

            publisher.subscribe(subscriber, parameters);
        } else {
            // Remote
            Link link = registryService.getServiceLink(streamReference).orElseThrow(IllegalArgumentException::new);

            try {
                URI websocketEndpointUri = URI.create(link.getHrefAsUriTemplate().createURI(parameters));
                webSocketClient.connect(new ReactiveStreamClientWebSocketEndpoint(streamReference, (ReactiveEventStreams.Subscriber<Object>) subscriber), websocketEndpointUri);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    record PublisherAndSerializer<T>(Publisher<T> publisher, EventHandler<Event<T>> serializer) {

    }
}
