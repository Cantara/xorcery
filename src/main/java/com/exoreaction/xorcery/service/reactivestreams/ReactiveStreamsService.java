package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.StandardConfiguration;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams2;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.PublisherWebSocketServlet;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.SubscriberWebSocketServlet;
import com.exoreaction.xorcery.service.registry.api.Registry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.reactivestreams.Publisher;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Singleton
public class ReactiveStreamsService
        implements ReactiveStreams2, LifeCycle.Listener {

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
                context.register(ReactiveStreamsService.class, ReactiveStreams2.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final boolean allowLocal;
    private final ServletContextHandler servletContextHandler;
    private final jakarta.inject.Provider<Registry> registryService;
    private final WebSocketClient webSocketClient;
    private final StandardConfiguration configuration;
    private final MessageBodyWorkers messageBodyWorkers;
    private final ObjectMapper objectMapper;
    private final Timer timer;

    private final ByteBufferPool byteBufferPool;

    private final Map<String, Function<Configuration, ? extends Flow.Publisher<?>>> publishers = new ConcurrentHashMap<>();
    private final Map<String, Function<Configuration, Flow.Subscriber<?>>> subscribers = new ConcurrentHashMap<>();

    private final List<CompletableFuture<Void>> activeSubscribeProcesses = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<Void>> activePublishProcesses = new CopyOnWriteArrayList<>();
    private final List<Flow.Subscription> activeSubscriptions = new CopyOnWriteArrayList<>();

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
        this.configuration = new StandardConfiguration.Impl(configuration);
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
        for (Flow.Subscription activeSubscription : activeSubscriptions) {
            activeSubscription.cancel();
            // TODO Populate corresponding CompletableFuture
        }

        logger.info("Cancel active subscribe processes:" + activeSubscribeProcesses.size());
        for (CompletableFuture<Void> activeSubscriptionProcess : activeSubscribeProcesses) {
            if (!activeSubscriptionProcess.isDone()) {
                activeSubscriptionProcess.cancel(true);
            }
            activeSubscriptionProcess.orTimeout(100, TimeUnit.SECONDS)
                    .whenComplete((r, t) ->
                    {
//                        logger.info("Subscription process whenComplete {} {}", r, t);
                        if (t != null && !(t instanceof CancellationException)) {
                            logger.error("Subscribe process ended with error", t);
                        }
                    });
        }

        logger.info("Cancel active publish processes:" + activePublishProcesses.size());
        for (CompletableFuture<Void> activePublishProcess : activePublishProcesses) {
            if (!activePublishProcess.isDone()) {
                activePublishProcess.cancel(true);
            }
            activePublishProcess.orTimeout(100, TimeUnit.SECONDS)
                    .whenComplete((r, t) ->
                    {
//                        logger.info("Subscription process whenComplete {} {}", r, t);
                        if (t != null && !(t instanceof CancellationException)) {
                            logger.error("Publish process ended with error", t);
                        }
                    });
        }

        logger.info("Shutdown reactive services client");
        try {
            webSocketClient.stop();
        } catch (Exception e) {
            logger.warn("Could not stop websocket client", e);
        }
    }

    // Server
    @Override
    public CompletionStage<Void> publisher(String publisherWebsocketPath,
                                           Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory) {
        publishers.put(publisherWebsocketPath, publisherFactory);

        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscriptions to this publisher. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            publishers.remove(publisherWebsocketPath);
        });

        MessageBodyWriter<Object> writer = getPublisherFactoryEventWriter(publisherFactory);
        MessageBodyReader<Object> reader = getPublisherFactoryResultReader(publisherFactory).orElse(null);
        Type resultType = getResultType(publisherFactory);

        PublisherWebSocketServlet servlet = new PublisherWebSocketServlet(publisherWebsocketPath, config -> new PublisherTracker((Flow.Publisher<Object>) publisherFactory.apply(config)), writer, reader, resultType, configuration.configuration(), objectMapper, byteBufferPool);

        servletContextHandler.addServlet(new ServletHolder(servlet), publisherWebsocketPath);
        logger.info("Added publisher websocket for " + publisherWebsocketPath);

        // TODO Shutdown the above on completable cancel
        return new CompletableFuture<>();
    }

    @Override
    public CompletionStage<Void> subscriber(String subscriberWebsocketPath,
                                            Function<Configuration, Flow.Subscriber<?>> subscriberFactory) {

        subscribers.put(subscriberWebsocketPath, subscriberFactory);

        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscription itself. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            subscribers.remove(subscriberWebsocketPath);
        });

        MessageBodyReader<Object> reader = getSubscriberFactoryEventReader(subscriberFactory);
        MessageBodyWriter<Object> writer = getSubscriberFactoryResultWriter(subscriberFactory).orElse(null);
        Type resultType = null;

        SubscriberWebSocketServlet servlet = new SubscriberWebSocketServlet(subscriberWebsocketPath, config -> new SubscriberTracker((Flow.Subscriber<Object>) subscriberFactory.apply(config)), writer, reader, resultType, configuration.configuration(), objectMapper, byteBufferPool);

        servletContextHandler.addServlet(new ServletHolder(servlet), subscriberWebsocketPath);
        logger.info("Added subscriber websocket for " + subscriberWebsocketPath);

        // TODO Shutdown the above on completable cancel
        return result;
    }

    // Client


    @Override
    public CompletionStage<Void> publish(URI subscriberWebsocketUri, Configuration subscriberConfiguration, Flow.Publisher<?> publisher) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the publishing process itself. Need to ensure they are cancelled on shutdown
        result.whenComplete((r, t) ->
        {
            activePublishProcesses.remove(result);
        });
        activePublishProcesses.add(result);

        if (allowLocal && subscriberWebsocketUri.getHost().equals(configuration.getHost())) {
            Function<Configuration, Flow.Subscriber<?>> subscriberFactory = subscribers.get(subscriberWebsocketUri.getPath());

            if (subscriberFactory != null) {
                // Local
                publisher.subscribe((Flow.Subscriber<Object>) subscriberFactory.apply(subscriberConfiguration));
                return result;
            }
        }

        MessageBodyWriter<Object> writer = getPublisherEventWriter(publisher);
        MessageBodyReader<Object> reader = getPublisherResultReader(publisher).orElse(null);
        Type resultType = getResultType(publisher);

        // Publisher wrapper so we can track active subscriptions
        publisher = new PublisherTracker((Flow.Publisher<Object>) publisher);

        // Start publishing process
        new PublishingProcess(webSocketClient, objectMapper, timer, logger, byteBufferPool,
                reader, writer,
                subscriberWebsocketUri, subscriberConfiguration,
                (Flow.Publisher<Object>) publisher,
                result).start();
        return result;
    }

    @Override
    public CompletionStage<Void> subscribe(URI publisherWebsocketUri, Configuration publisherConfiguration, Flow.Subscriber<?> subscriber) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        // Track the subscription process itself. Need to ensure they are cancelled on shutdown
        result.whenComplete((r, t) ->
        {
            activeSubscribeProcesses.remove(result);
        });
        activeSubscribeProcesses.add(result);

        if (allowLocal && publisherWebsocketUri.getHost().equals(configuration.getHost())) {
            Function<Configuration, ? extends Flow.Publisher<?>> publisher = publishers.get(publisherWebsocketUri.getPath());

            if (publisher != null) {
                // Local
                publisher.apply(publisherConfiguration).subscribe((Flow.Subscriber<Object>) subscriber);
                return result;
            }
        }

        MessageBodyReader<Object> reader = getSubscriberEventReader(subscriber);
        MessageBodyWriter<Object> writer = getSubscriberResultWriter(subscriber).orElse(null);
        Type resultType = getResultType(subscriber);

        // Subscriber wrapper so we can track active subscriptions
        subscriber = new SubscriberTracker((Flow.Subscriber<Object>) subscriber);

        // Start subscription process
        new SubscriptionProcess(webSocketClient, objectMapper, timer, logger, byteBufferPool,
                reader, writer,
                publisherWebsocketUri,
                publisherConfiguration,
                (Flow.Subscriber<Object>) subscriber,
                result).start();
        return result;
    }

    private Type getResultType(Object value) {
        return null; // TODO
    }

    private MessageBodyWriter<Object> getPublisherFactoryEventWriter(Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory) {
        return Stream.of(publisherFactory.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(Function.class))
                .map(ParameterizedType.class::cast) // Function<C,Publisher<T>>
                .map(pt -> pt.getActualTypeArguments()[1])
                .map(Class.class::cast)
                .flatMap(t -> Stream.of(t.getGenericInterfaces()))
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(Flow.Publisher.class))
                .map(ParameterizedType.class::cast) // Publisher<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .map(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type) // T or WithResult<T,R>
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyWriter((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyWriter for " + type));
                    } else {
                        return null;
                    }
                }).findFirst().orElseThrow(() ->
                {
                    return new IllegalArgumentException("Wrong type for publisherfactory");
                });
    }

    private Optional<MessageBodyReader<Object>> getPublisherFactoryResultReader(Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory) {
        return Stream.of(publisherFactory.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(Function.class))
                .map(ParameterizedType.class::cast) // Function<C,Publisher<T>>
                .map(pt -> pt.getActualTypeArguments()[1])
                .map(Class.class::cast)
                .flatMap(t -> Stream.of(t.getGenericInterfaces()))
                .map(ParameterizedType.class::cast) // Publisher<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class))
                .map(ParameterizedType.class::cast) // WithResult<T,R>
                .map(pt -> pt.getActualTypeArguments()[1]) // R
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyReader((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyReader for " + type));
                    } else {
                        return null;
                    }
                }).findFirst();
    }

    private MessageBodyReader<Object> getSubscriberFactoryEventReader(Function<Configuration, Flow.Subscriber<?>> subscriberFactory) {
        return Stream.of(subscriberFactory.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(Function.class))
                .map(ParameterizedType.class::cast) // Function<C,Subscriber<T>>
                .map(pt -> pt.getActualTypeArguments()[1])
                .map(ParameterizedType.class::cast) // Subscriber<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .map(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type) // T or WithResult<T,R>
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyReader((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyWriter for " + type));
                    } else {
                        return null;
                    }
                }).findFirst().orElseThrow(() -> new IllegalArgumentException("Wrong type for subscriberfactory:" + subscriberFactory.getClass()));
    }

    private Optional<MessageBodyWriter<Object>> getSubscriberFactoryResultWriter(Function<Configuration, Flow.Subscriber<?>> subscriberFactory) {
        return Stream.of(subscriberFactory.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(Function.class))
                .map(ParameterizedType.class::cast) // Function<C,Publisher<T>>
                .map(pt -> pt.getActualTypeArguments()[1])
                .map(ParameterizedType.class::cast) // Publisher<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class))
                .map(ParameterizedType.class::cast) // WithResult<T,R>
                .map(pt -> pt.getActualTypeArguments()[1]) // R
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyWriter((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyReader for " + type));
                    } else {
                        return null;
                    }
                }).findFirst();
    }

    private MessageBodyWriter<Object> getPublisherEventWriter(Flow.Publisher<?> publisher) {
        return Stream.of(publisher.getClass().getGenericInterfaces())
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(Publisher.class))
                .map(ParameterizedType.class::cast) // Publisher<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .map(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type) // T or WithResult<T,R>
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyWriter((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyWriter for " + type));
                    } else {
                        return null;
                    }
                }).findFirst().orElseThrow(() -> new IllegalArgumentException("Wrong type for publisherfactory"));
    }

    private Optional<MessageBodyReader<Object>> getPublisherResultReader(Flow.Publisher<?> publisherFactory) {
        return Stream.of(publisherFactory.getClass().getGenericInterfaces())
                .map(ParameterizedType.class::cast) // Publisher<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class))
                .map(ParameterizedType.class::cast) // WithResult<T,R>
                .map(pt -> pt.getActualTypeArguments()[1]) // R
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyReader((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyReader for " + type));
                    } else {
                        return null;
                    }
                }).findFirst();
    }

    private MessageBodyReader<Object> getSubscriberEventReader(Flow.Subscriber<?> subscriber) {
        return Stream.of(subscriber.getClass().getGenericInterfaces())
                .map(ParameterizedType.class::cast) // Subscriber<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .map(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type) // T or WithResult<T,R>
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyReader((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyWriter for " + type));
                    } else {
                        return null;
                    }
                }).findFirst().orElseThrow(() -> new IllegalArgumentException("Wrong type for subscriberfactory:" + subscriber.getClass()));
    }

    private Optional<MessageBodyWriter<Object>> getSubscriberResultWriter(Flow.Subscriber<?> subscriber) {
        return Stream.of(subscriber.getClass().getGenericInterfaces())
                .map(ParameterizedType.class::cast) // Subscriber<T>
                .map(pt -> pt.getActualTypeArguments()[0])
                .filter(type -> type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class))
                .map(ParameterizedType.class::cast) // WithResult<T,R>
                .map(pt -> pt.getActualTypeArguments()[1]) // R
                .map(type ->
                {
                    if (!type.equals(ByteBuffer.class)) {
                        return Optional.ofNullable(messageBodyWorkers.getMessageBodyWriter((Class<Object>) type, type, new Annotation[0], MediaType.WILDCARD_TYPE))
                                .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyReader for " + type));
                    } else {
                        return null;
                    }
                }).findFirst();
    }

    private class SubscriberTracker implements Flow.Subscriber<Object> {
        private final Flow.Subscriber<Object> subscriber;
        private Flow.Subscription subscription;

        public SubscriberTracker(Flow.Subscriber<Object> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscriber.onSubscribe(subscription);
        }

        @Override
        public void onNext(Object item) {
            subscriber.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            activeSubscriptions.remove(subscription);
            subscriber.onError(throwable);
        }

        @Override
        public void onComplete() {
            activeSubscriptions.remove(subscription);
            subscriber.onComplete();
        }
    }

    private class PublisherTracker implements Flow.Publisher<Object> {
        private final Flow.Publisher<Object> publisher;

        public PublisherTracker(Flow.Publisher<Object> publisher) {
            this.publisher = publisher;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super Object> subscriber) {
            publisher.subscribe(new SubscriberTracker(subscriber));
        }
    }
}
