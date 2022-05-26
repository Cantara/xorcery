package com.exoreaction.reactiveservices.service.reactivestreams;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket.PublisherWebSocketServlet;
import com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket.SubscriberWebSocketEndpoint;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventSink;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Singleton
public class ReactiveStreamsService
        implements ReactiveStreams, ContainerLifecycleListener {

    private static final Logger logger = LogManager.getLogger(ReactiveStreamsService.class);
    public static final String SERVICE_TYPE = "reactivestreams";

    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void configure() {
            try {
                HttpClient httpClient = new HttpClient();
                WebSocketClient webSocketClient = new WebSocketClient(httpClient);
                webSocketClient.setIdleTimeout(Duration.ofMillis(Long.MAX_VALUE));
                webSocketClient.start();
                bind(webSocketClient).named(SERVICE_TYPE);
                context.register(ReactiveStreamsService.class, ReactiveStreams.class, ContainerLifecycleListener.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final boolean allowLocal;
    private ServletContextHandler servletContextHandler;
    private jakarta.inject.Provider<Registry> registryService;
    private WebSocketClient webSocketClient;
    private MessageBodyWorkers messageBodyWorkers;
    private ObjectMapper objectMapper;

    private ByteBufferPool byteBufferPool;

    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    private final List<CompletableFuture<Void>> activeSubscriptionProcesses = new CopyOnWriteArrayList<>();
    private final List<ReactiveEventStreams.Subscription> activeSubscriptions = new CopyOnWriteArrayList<>();

    @Inject
    public ReactiveStreamsService(ServletContextHandler servletContextHandler,
                                  @Named(SERVICE_TYPE) WebSocketClient webSocketClient,
                                  jakarta.inject.Provider<Registry> registryService,
                                  Configuration configuration,
                                  MessageBodyWorkers messageBodyWorkers,
                                  ObjectMapper objectMapper) {
        this.servletContextHandler = servletContextHandler;
        this.webSocketClient = webSocketClient;
        this.registryService = registryService;
        this.messageBodyWorkers = messageBodyWorkers;
        this.objectMapper = objectMapper;
        this.allowLocal = configuration.getBoolean("reactivestreams.allowlocal").orElse(true);

        byteBufferPool = new ArrayByteBufferPool();
    }

    @Override
    public void onStartup(Container container) {
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        logger.info("SHUTDOWN REACTIVE SERVICES!");
        logger.info("Cancel active subscriptions:" + activeSubscriptions.size());

        // Cancel active subscriptions
        for (ReactiveEventStreams.Subscription activeSubscription : activeSubscriptions) {
            activeSubscription.cancel();
        }

        logger.info("Cancel active subscription processes:" + activeSubscriptionProcesses.size());
        for (CompletableFuture<Void> activeSubscriptionProcess : activeSubscriptionProcesses) {
            if (!activeSubscriptionProcess.isDone()) {
                activeSubscriptionProcess.cancel(true);
            }
            activeSubscriptionProcess.orTimeout(100, TimeUnit.SECONDS)
                    .whenComplete((r, t) ->
                    {
//                        logger.info("Subscription process whenComplete {} {}", r, t);
                        if (t != null && !(t instanceof CancellationException)) {
                            logger.error("Subscription process ended with error", t);
                        }
                    });
        }

        logger.info("SHUTDOWN REACTIVE SERVICES CLIENT!");
        try {
            webSocketClient.stop();
        } catch (Exception e) {
            logger.warn("Could not stop websocket client", e);
        }
    }

    public <T> void publish(ServiceIdentifier selfServiceIdentifier, Link websocketLink, Publisher<T> publisher) {
        publishers.put(websocketLink.getHref(), publisher);

        MessageBodyWriter<Object> writer = null;
        MessageBodyReader<Object> reader = null;
        Type resultType = null;

        for (Type genericInterface : publisher.getClass().getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(Publisher.class)) {
                ParameterizedType publisherType = ((ParameterizedType) genericInterface);
                Type publisherEventType = publisherType.getActualTypeArguments()[0];
                if (publisherEventType instanceof ParameterizedType && ((ParameterizedType) publisherEventType).getRawType().equals(EventWithResult.class)) {
                    ParameterizedType eventWithResultType = ((ParameterizedType) publisherEventType);
                    Type eventType = eventWithResultType.getActualTypeArguments()[0];
                    resultType = eventWithResultType.getActualTypeArguments()[1];
                    writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>) eventType, eventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (writer == null) {
                        throw new IllegalStateException("Could not find MessageBodyWriter for " + eventType);
                    }

                    reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) resultType, resultType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (reader == null) {
                        throw new IllegalStateException("Could not find MessageBodyReader for " + resultType);
                    }

                } else {
                    writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>) publisherEventType, publisherEventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    if (writer == null) {
                        throw new IllegalStateException("Could not find MessageBodyWriter for " + publisherEventType);
                    }
                }
            }
        }

        if (writer == null) {
            throw new IllegalStateException("Could not find MessageBodyWriter for " + publisher.getClass());
        }

        String path = URI.create(websocketLink.getHrefAsUriTemplate().createURI()).getPath();
        PublisherWebSocketServlet<T> servlet = new PublisherWebSocketServlet<T>(path, new PublisherTracker<>(publisher), writer, reader, resultType, objectMapper);

        servletContextHandler.addServlet(new ServletHolder(servlet), path);
        logger.info("Published websocket for " + selfServiceIdentifier);
    }

    public <T> CompletionStage<Void> subscribe(ServiceIdentifier selfServiceIdentifier,
                                               Link websocketLink,
                                               ReactiveEventStreams.Subscriber<T> subscriber,
                                               Map<String, String> parameters) {

        CompletableFuture<Void> result = new CompletableFuture<>();

        // Track the subscription process itself. Need to ensure they are cancelled on shutdown
        result.whenComplete((r, t) ->
        {
            activeSubscriptionProcesses.remove(result);
        });
        activeSubscriptionProcesses.add(result);

        Publisher<T> publisher = publishers.get(websocketLink.getHref());

        if (allowLocal && publisher != null) {
            // Local
            publisher.subscribe(subscriber, parameters);
            return result;
        } else {

            MessageBodyWriter<Object> writer = null;
            MessageBodyReader<Object> reader = null;
            Type eventType = null;

            for (Type genericInterface : subscriber.getClass().getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(ReactiveEventStreams.Subscriber.class)) {
                    ParameterizedType subscriberType = ((ParameterizedType) genericInterface);
                    Type subscriberEventType = subscriberType.getActualTypeArguments()[0];
                    if (subscriberEventType instanceof ParameterizedType && ((ParameterizedType) subscriberEventType).getRawType().equals(EventWithResult.class)) {
                        ParameterizedType eventWithResultType = ((ParameterizedType) subscriberEventType);
                        eventType = eventWithResultType.getActualTypeArguments()[0];
                        Type resultType = eventWithResultType.getActualTypeArguments()[1];
                        reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) eventType, eventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                        if (reader == null) {
                            result.completeExceptionally(new IllegalStateException("Could not find MessageBodyReader for " + eventType));
                            return result;
                        }

                        writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>) resultType, resultType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                        if (writer == null) {
                            result.completeExceptionally(new IllegalStateException("Could not find MessageBodyWriter for " + resultType));
                            return result;
                        }

                    } else {
                        eventType = subscriberEventType;
                        reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) subscriberEventType, subscriberEventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
                        if (reader == null) {
                            result.completeExceptionally(new IllegalStateException("Could not find MessageBodyReader for " + subscriberEventType));
                            return result;
                        }
                    }
                }
            }

            if (reader == null) {
                result.completeExceptionally(new IllegalStateException("Could not find MessageBodyReader for " + subscriber.getClass()));
                return result;
            }

            // Subscriber wrapper so we can track active subscriptions
            subscriber = new SubscriberTracker<T>(subscriber);

            // Start subscription process
            new SubscriptionProcess<T>(webSocketClient, objectMapper, byteBufferPool,
                    reader, writer,
                    selfServiceIdentifier, websocketLink, parameters,
                    subscriber, eventType,
                    result).start();
            return result;
        }
    }

    public record SubscriptionProcess<T>(WebSocketClient webSocketClient, ObjectMapper objectMapper,
                                         ByteBufferPool byteBufferPool, MessageBodyReader<Object> reader,
                                         MessageBodyWriter<Object> writer, ServiceIdentifier selfServiceIdentifier,
                                         Link websocketLink,
                                         Map<String, String> parameters,
                                         ReactiveEventStreams.Subscriber<T> subscriber,
                                         Type eventType, CompletableFuture<Void> result
    ) {
        public void start() {
            if (result.isDone()) {
                return;
            }

            if (!webSocketClient.isStarted()) {
                retry();
            }

            ForkJoinPool.commonPool().execute(() ->
            {
                try {
                    Marker marker = MarkerManager.getMarker(selfServiceIdentifier.toString());

                    URI websocketEndpointUri = URI.create(websocketLink.getHrefAsUriTemplate().createURI(parameters));
                    webSocketClient.connect(new SubscriberWebSocketEndpoint<T>(subscriber, reader, writer, objectMapper, eventType, marker,
                                    byteBufferPool, this, websocketEndpointUri.toASCIIString()), websocketEndpointUri)
                            .whenComplete(this::complete);
                } catch (IOException e) {
                    logger.error("Could not subscribe to " + websocketLink.getHref(), e);

                    retry();
                }
            });
        }

        private void complete(Session session, Throwable throwable) {
            if (throwable != null) {
                logger.error("Could not subscribe to " + websocketLink.getHref(), throwable);
                retry();
            }
        }

        private void retry() {
            // TODO Clever wait and retry logic goes here.
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                // Ignore
            }

            start();
        }
    }

    private class SubscriberTracker<T> implements ReactiveEventStreams.Subscriber<T> {
        private ReactiveEventStreams.Subscriber<T> subscriber;
        private SubscriptionTracker trackedSubscription;

        public SubscriberTracker(ReactiveEventStreams.Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public EventSink<Event<T>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            trackedSubscription = new SubscriptionTracker(subscription);
            activeSubscriptions.add(trackedSubscription);
            return subscriber.onSubscribe(trackedSubscription);
        }

        @Override
        public void onError(Throwable throwable) {
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
            activeSubscriptions.remove(trackedSubscription);
        }

        private class SubscriptionTracker implements ReactiveEventStreams.Subscription {
            private ReactiveEventStreams.Subscription subscription;

            public SubscriptionTracker(ReactiveEventStreams.Subscription subscription) {
                this.subscription = subscription;
            }

            @Override
            public void request(long n) {
                subscription.request(n);
            }

            @Override
            public void cancel() {
                subscription.cancel();
            }
        }
    }

    private class PublisherTracker<T> implements ReactiveEventStreams.Publisher<T> {
        private Publisher<T> publisher;

        public PublisherTracker(ReactiveEventStreams.Publisher<T> publisher) {
            this.publisher = publisher;
        }

        @Override
        public void subscribe(ReactiveEventStreams.Subscriber<T> subscriber, Map<String, String> parameters) {
            publisher.subscribe(new SubscriberTracker<>(subscriber), parameters);
        }
    }
}
