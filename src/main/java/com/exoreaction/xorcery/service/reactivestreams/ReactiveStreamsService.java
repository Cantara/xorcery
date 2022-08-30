package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams.Subscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.PublisherWebSocketServlet;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.SubscriberWebSocketServlet;
import com.exoreaction.xorcery.service.registry.api.Registry;
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
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.glassfish.jersey.message.MessageBodyWorkers;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.*;

@Singleton
public class ReactiveStreamsService
        implements ReactiveStreams, LifeCycle.Listener {

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
                HttpClient httpClient = injectionManager.getInstance(JettyHttpClientSupplier.class).getHttpClient();
                WebSocketClient webSocketClient = new WebSocketClient(httpClient);
                webSocketClient.setIdleTimeout(Duration.ofSeconds(httpClient.getIdleTimeout()));
                webSocketClient.start();
                bind(webSocketClient).named(SERVICE_TYPE);
                context.register(ReactiveStreamsService.class, ReactiveStreams.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final boolean allowLocal;
    private final ServletContextHandler servletContextHandler;
    private final jakarta.inject.Provider<Registry> registryService;
    private final WebSocketClient webSocketClient;
    private final Configuration configuration;
    private final MessageBodyWorkers messageBodyWorkers;
    private final ObjectMapper objectMapper;
    private final Timer timer;

    private final ByteBufferPool byteBufferPool;

    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();
    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();

    private final List<CompletableFuture<Void>> activeSubscriptionProcesses = new CopyOnWriteArrayList<>();
    private final List<ReactiveEventStreams.Subscription> activeSubscriptions = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<Void>> activePublishingProcesses = new CopyOnWriteArrayList<>();

    @Inject
    public ReactiveStreamsService(ServletContextHandler servletContextHandler,
                                  @Named(SERVICE_TYPE) WebSocketClient webSocketClient,
                                  jakarta.inject.Provider<Registry> registryService,
                                  Configuration configuration,
                                  Server server,
                                  MessageBodyWorkers messageBodyWorkers,
                                  ObjectMapper objectMapper) {
        this.servletContextHandler = servletContextHandler;
        this.webSocketClient = webSocketClient;
        this.registryService = registryService;
        this.configuration = configuration;
        this.messageBodyWorkers = messageBodyWorkers;
        this.objectMapper = objectMapper;
        this.allowLocal = configuration.getBoolean("reactivestreams.allowlocal").orElse(true);
        this.timer = new Timer();

        server.addEventListener(this);

        byteBufferPool = new ArrayByteBufferPool();
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
        logger.info("SHUTDOWN REACTIVE SERVICES!");

        timer.cancel();

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

    public <T> CompletionStage<Void> publisher(ServiceIdentifier selfServiceIdentifier, Link websocketLink, Publisher<T> publisher) {
        publishers.put(websocketLink.getHref(), publisher);

        MessageBodyWriter<T> writer = null;
        MessageBodyReader<Object> reader = null;
        Type resultType = null;

        Type publisherEventType = null;
        for (Type genericInterface : publisher.getClass().getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(Publisher.class)) {
                ParameterizedType publisherType = ((ParameterizedType) genericInterface);
                publisherEventType = publisherType.getActualTypeArguments()[0];
                if (publisherEventType instanceof ParameterizedType && ((ParameterizedType) publisherEventType).getRawType().equals(EventWithResult.class)) {
                    ParameterizedType eventWithResultType = ((ParameterizedType) publisherEventType);
                    Type eventType = eventWithResultType.getActualTypeArguments()[0];
                    resultType = eventWithResultType.getActualTypeArguments()[1];

                    if (!eventType.equals(ByteBuffer.class)) {
                        writer = (MessageBodyWriter<T>) messageBodyWorkers.getMessageBodyWriter((Class<T>) eventType, eventType, new Annotation[0], MediaType.WILDCARD_TYPE);
                        if (writer == null) {
                            throw new IllegalStateException("Could not find MessageBodyWriter for " + eventType);
                        }
                    }

                    reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) resultType, resultType, new Annotation[0], MediaType.WILDCARD_TYPE);
                    if (reader == null) {
                        throw new IllegalStateException("Could not find MessageBodyReader for " + resultType);
                    }

                } else {
                    if (!publisherEventType.equals(ByteBuffer.class)) {
                        writer = (MessageBodyWriter<T>) messageBodyWorkers.getMessageBodyWriter((Class<T>) publisherEventType, publisherEventType, new Annotation[0], MediaType.WILDCARD_TYPE);
                        if (writer == null) {
                            throw new IllegalStateException("Could not find MessageBodyWriter for " + publisherEventType);
                        }
                    }
                }
            }
        }

        if (writer == null && !ByteBuffer.class.equals(publisherEventType)) {
            throw new IllegalStateException("Could not find MessageBodyWriter for " + publisher.getClass()+" (type "+publisherEventType.getTypeName()+")");
        }

        String path = URI.create(websocketLink.getHrefAsUriTemplate().createURI()).getPath();
        PublisherWebSocketServlet<T> servlet = new PublisherWebSocketServlet<T>(path, new PublisherTracker<>(publisher), writer, reader, resultType, configuration, objectMapper, byteBufferPool, MarkerManager.getMarker(selfServiceIdentifier.toString()));

        servletContextHandler.addServlet(new ServletHolder(servlet), path);
        logger.info("Added publisher websocket for " + selfServiceIdentifier);

        // TODO Shutdown the above on completable cancel
        return new CompletableFuture<>();
    }

    public <T> CompletionStage<Void> subscribe(ServiceIdentifier selfServiceIdentifier,
                                               Link websocketLink,
                                               Subscriber<T> subscriber,
                                               Configuration publisherConfiguration,
                                               Configuration subscriberConfiguration) {

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
            publisher.subscribe(subscriber, publisherConfiguration);
            return result;
        } else {

            MessageBodyWriter<Object> writer = null;
            MessageBodyReader<Object> reader = null;
            Type eventType = null;

            for (Type genericInterface : subscriber.getClass().getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(Subscriber.class)) {
                    ParameterizedType subscriberType = ((ParameterizedType) genericInterface);
                    Type subscriberEventType = subscriberType.getActualTypeArguments()[0];
                    if (subscriberEventType instanceof ParameterizedType && ((ParameterizedType) subscriberEventType).getRawType().equals(EventWithResult.class)) {
                        ParameterizedType eventWithResultType = ((ParameterizedType) subscriberEventType);
                        eventType = eventWithResultType.getActualTypeArguments()[0];
                        Type resultType = eventWithResultType.getActualTypeArguments()[1];
                        reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) eventType, eventType, new Annotation[0], MediaType.WILDCARD_TYPE);
                        if (reader == null) {
                            result.completeExceptionally(new IllegalStateException("Could not find MessageBodyReader for " + eventType));
                            return result;
                        }

                        writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>) resultType, resultType, new Annotation[0], MediaType.WILDCARD_TYPE);
                        if (writer == null) {
                            result.completeExceptionally(new IllegalStateException("Could not find MessageBodyWriter for " + resultType));
                            return result;
                        }

                    } else {
                        eventType = subscriberEventType;
                        reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) subscriberEventType, subscriberEventType, new Annotation[0], MediaType.WILDCARD_TYPE);
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
            new SubscriptionProcess<T>(webSocketClient, objectMapper, timer, logger, byteBufferPool,
                    reader, writer,
                    selfServiceIdentifier, websocketLink, publisherConfiguration,subscriberConfiguration,
                    subscriber, eventType,
                    result).start();
            return result;
        }
    }

    @Override
    public <T> CompletionStage<Void> subscriber(ServiceIdentifier selfServiceIdentifier,
                                                Link subscriberWebsocketLink,
                                                Subscriber<T> subscriber) {

        subscribers.put(subscriberWebsocketLink.getHref(), subscriber);

        CompletableFuture<Void> result = new CompletableFuture<>();

        // Track the subscription process itself. Need to ensure they are cancelled on shutdown
        result.whenComplete((r, t) ->
        {
            subscribers.remove(subscriberWebsocketLink.getHref());
        });

        MessageBodyWriter<Object> writer = null;
        MessageBodyReader<Object> reader = null;
        Type eventType = null;

        for (Type genericInterface : subscriber.getClass().getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(Subscriber.class)) {
                ParameterizedType subscriberType = ((ParameterizedType) genericInterface);
                Type subscriberEventType = subscriberType.getActualTypeArguments()[0];
                if (subscriberEventType instanceof ParameterizedType && ((ParameterizedType) subscriberEventType).getRawType().equals(EventWithResult.class)) {
                    ParameterizedType eventWithResultType = ((ParameterizedType) subscriberEventType);
                    eventType = eventWithResultType.getActualTypeArguments()[0];
                    Type resultType = eventWithResultType.getActualTypeArguments()[1];
                    reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) eventType, eventType, new Annotation[0], MediaType.WILDCARD_TYPE);
                    if (reader == null) {
                        result.completeExceptionally(new IllegalStateException("Could not find MessageBodyReader for " + eventType));
                        return result;
                    }

                    writer = (MessageBodyWriter<Object>) messageBodyWorkers.getMessageBodyWriter((Class<?>) resultType, resultType, new Annotation[0], MediaType.WILDCARD_TYPE);
                    if (writer == null) {
                        result.completeExceptionally(new IllegalStateException("Could not find MessageBodyWriter for " + resultType));
                        return result;
                    }

                } else {
                    eventType = subscriberEventType;
                    reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) subscriberEventType, subscriberEventType, new Annotation[0], MediaType.WILDCARD_TYPE);
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

        String path = URI.create(subscriberWebsocketLink.getHrefAsUriTemplate().createURI()).getPath();
        SubscriberWebSocketServlet<T> servlet = new SubscriberWebSocketServlet<T>(path, new SubscriberTracker<>(subscriber), writer, reader, eventType, configuration, objectMapper, byteBufferPool, MarkerManager.getMarker(selfServiceIdentifier.toString()));

        servletContextHandler.addServlet(new ServletHolder(servlet), path);
        logger.info("Added subscriber websocket for " + selfServiceIdentifier);

        // TODO Shutdown the above on completable cancel
        return result;
    }

    @Override
    public <T> CompletionStage<Void> publish(ServiceIdentifier selfServiceIdentifier,
                                             Link subscriberWebsocketLink,
                                             Publisher<T> publisher,
                                             Configuration publisherConfiguration,
                                             Configuration subscriberConfiguration) {

        CompletableFuture<Void> result = new CompletableFuture<>();

        // Track the publishing process itself. Need to ensure they are cancelled on shutdown
        result.whenComplete((r, t) ->
        {
            activePublishingProcesses.remove(result);
        });
        activePublishingProcesses.add(result);

        Subscriber<T> subscriber = subscribers.get(subscriberWebsocketLink.getHref());

        if (allowLocal && subscriber != null) {
            // Local
            publisher.subscribe(subscriber, publisherConfiguration);
            return result;
        } else {

            MessageBodyWriter<T> writer = null;
            MessageBodyReader<Object> reader = null;
            Type resultType = null;

            Type publisherEventType = null;
            for (Type genericInterface : publisher.getClass().getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType && ((ParameterizedType) genericInterface).getRawType().equals(Publisher.class)) {
                    ParameterizedType publisherType = ((ParameterizedType) genericInterface);
                    publisherEventType = publisherType.getActualTypeArguments()[0];
                    if (publisherEventType instanceof ParameterizedType && ((ParameterizedType) publisherEventType).getRawType().equals(EventWithResult.class)) {
                        ParameterizedType eventWithResultType = ((ParameterizedType) publisherEventType);
                        Type eventType = eventWithResultType.getActualTypeArguments()[0];
                        resultType = eventWithResultType.getActualTypeArguments()[1];

                        if (!eventType.equals(ByteBuffer.class)) {
                            writer = (MessageBodyWriter<T>) messageBodyWorkers.getMessageBodyWriter((Class<T>) eventType, eventType, new Annotation[0], MediaType.WILDCARD_TYPE);
                            if (writer == null) {
                                throw new IllegalStateException("Could not find MessageBodyWriter for " + eventType);
                            }
                        }

                        reader = (MessageBodyReader<Object>) messageBodyWorkers.getMessageBodyReader((Class<?>) resultType, resultType, new Annotation[0], MediaType.WILDCARD_TYPE);
                        if (reader == null) {
                            throw new IllegalStateException("Could not find MessageBodyReader for " + resultType);
                        }

                    } else {
                        if (!publisherEventType.equals(ByteBuffer.class)) {
                            writer = (MessageBodyWriter<T>) messageBodyWorkers.getMessageBodyWriter((Class<T>) publisherEventType, publisherEventType, new Annotation[0], MediaType.WILDCARD_TYPE);
                            if (writer == null) {
                                throw new IllegalStateException("Could not find MessageBodyWriter for " + publisherEventType);
                            }
                        }
                    }
                }
            }

            if (writer == null && !ByteBuffer.class.equals(publisherEventType)) {
                throw new IllegalStateException("Could not find MessageBodyWriter for " + publisher.getClass()+" (type "+publisherEventType.getTypeName()+")");
            }

            // Publisher wrapper so we can track active subscriptions
            publisher = new PublisherTracker<T>(publisher);

            // Start publishing process
            new PublishingProcess<T>(webSocketClient, objectMapper, timer, logger, byteBufferPool,
                    reader, writer,
                    selfServiceIdentifier, subscriberWebsocketLink, publisherConfiguration,subscriberConfiguration,
                    publisher, publisherEventType,
                    result).start();
            return result;
        }
    }

    private class SubscriberTracker<T> implements Subscriber<T> {
        private Subscriber<T> subscriber;
        private SubscriptionTracker trackedSubscription;

        public SubscriberTracker(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public EventSink<Event<T>> onSubscribe(ReactiveEventStreams.Subscription subscription, Configuration configuration) {
            trackedSubscription = new SubscriptionTracker(subscription);
            activeSubscriptions.add(trackedSubscription);
            return subscriber.onSubscribe(trackedSubscription, configuration);
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
        public void subscribe(Subscriber<T> subscriber, Configuration parameters) {
            publisher.subscribe(new SubscriberTracker<>(subscriber), parameters);
        }
    }
}
