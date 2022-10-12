package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.StandardConfiguration;
import com.exoreaction.xorcery.jersey.AbstractFeature;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.PublisherWebSocketServlet;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.SubscriberWebSocketServlet;
import com.exoreaction.xorcery.util.Classes;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

@Singleton
public class ReactiveStreamsService
        implements ReactiveStreams, LifeCycle.Listener {

    private static final Logger logger = LogManager.getLogger(ReactiveStreamsService.class);
    public static final String SERVICE_TYPE = "reactivestreams";

    // Magic bytes for sending exceptions
    public static final byte[] XOR = "XOR".getBytes(StandardCharsets.UTF_8);

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
                                  Configuration configuration,
                                  Server server,
                                  MessageBodyWorkers messageBodyWorkers,
                                  ObjectMapper objectMapper) {
        this.servletContextHandler = servletContextHandler;
        this.webSocketClient = webSocketClient;
        this.configuration = ()->configuration;
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
                                           Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory,
                                           Class<? extends Flow.Publisher<?>> publisherType) {
        publishers.put(publisherWebsocketPath, publisherFactory);

        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscriptions to this publisher. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            publishers.remove(publisherWebsocketPath);
        });

        Type type = resolveActualTypeArgs(publisherType, Flow.Publisher.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageBodyWriter<Object> eventWriter = getWriter(eventType);
        MessageBodyReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

        PublisherWebSocketServlet servlet = new PublisherWebSocketServlet(
                publisherWebsocketPath,
                config -> new PublisherTracker((Flow.Publisher<Object>) publisherFactory.apply(config)),
                eventWriter,
                resultReader,
                eventType,
                resultType.orElse(null),
                configuration.context(),
                objectMapper,
                byteBufferPool);

        servletContextHandler.addServlet(new ServletHolder(servlet), publisherWebsocketPath);
        logger.info("Added publisher websocket for " + publisherWebsocketPath);

        // TODO Shutdown the above on completable cancel
        return new CompletableFuture<>();
    }

    @Override
    public CompletionStage<Void> subscriber(String subscriberWebsocketPath, Function<Configuration, Flow.Subscriber<?>> subscriberFactory, Class<? extends Flow.Subscriber<?>> subscriberType) {
        subscribers.put(subscriberWebsocketPath, subscriberFactory);

        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscription itself. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            subscribers.remove(subscriberWebsocketPath);
        });

        Type type = resolveActualTypeArgs(subscriberType, Flow.Subscriber.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageBodyReader<Object> eventReader = getReader(eventType);
        MessageBodyWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

        SubscriberWebSocketServlet servlet = new SubscriberWebSocketServlet(
                subscriberWebsocketPath,
                config -> new SubscriberTracker((Flow.Subscriber<Object>) subscriberFactory.apply(config)),
                resultWriter,
                eventReader,
                eventType,
                resultType.orElse(null),
                configuration.context(),
                objectMapper,
                byteBufferPool);

        servletContextHandler.addServlet(new ServletHolder(servlet), subscriberWebsocketPath);
        logger.info("Added subscriber websocket for " + subscriberWebsocketPath);

        // TODO Shutdown the above on completable cancel
        return result;
    }

    // Client
    @Override
    public CompletionStage<Void> publish(URI subscriberWebsocketUri, Configuration subscriberConfiguration, Flow.Publisher<?> publisher, Class<? extends Flow.Publisher<?>> publisherType) {
        if (publisherType == null)
            publisherType = (Class<? extends Flow.Publisher<?>>) publisher.getClass();

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

        Type type = resolveActualTypeArgs(publisherType, Flow.Publisher.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);

        MessageBodyWriter<Object> eventWriter = getWriter(eventType);
        MessageBodyReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

        // Publisher wrapper so we can track active subscriptions
        publisher = new PublisherTracker((Flow.Publisher<Object>) publisher);

        // Start publishing process
        new PublishingProcess(
                webSocketClient,
                objectMapper,
                timer,
                logger,
                byteBufferPool,
                resultReader,
                eventWriter,
                eventType,
                resultType.orElse(null),
                subscriberWebsocketUri,
                subscriberConfiguration,
                (Flow.Publisher<Object>) publisher,
                result).start();
        return result;
    }

    @Override
    public CompletionStage<Void> subscribe(URI publisherWebsocketUri, Configuration publisherConfiguration, Flow.Subscriber<?> subscriber, Class<? extends Flow.Subscriber<?>> subscriberType) {
        if (subscriberType == null)
            subscriberType = (Class<? extends Flow.Subscriber<?>>) subscriber.getClass();

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

        Type type = resolveActualTypeArgs(subscriberType, Flow.Subscriber.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);

        MessageBodyReader<Object> eventReader = getReader(eventType);
        MessageBodyWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

        // Subscriber wrapper so we can track active subscriptions
        subscriber = new SubscriberTracker((Flow.Subscriber<Object>) subscriber);

        // Start subscription process
        new SubscriptionProcess(
                webSocketClient,
                objectMapper,
                timer,
                logger,
                byteBufferPool,
                eventReader,
                resultWriter,
                eventType,
                resultType.orElse(null),
                publisherWebsocketUri,
                publisherConfiguration,
                (Flow.Subscriber<Object>) subscriber,
                result).start();
        return result;
    }

    private Type getEventType(Type type) {
        return type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type;
    }

    private Optional<Type> getResultType(Type type) {
        return Optional.ofNullable(type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[1] : null);
    }

    /**
     * From https://stackoverflow.com/questions/17297308/how-do-i-resolve-the-actual-type-for-a-generic-return-type-using-reflection
     * <p>
     * Resolves the actual generic type arguments for a base class, as viewed from a subclass or implementation.
     *
     * @param <T>        base type
     * @param offspring  class or interface subclassing or extending the base type
     * @param base       base class
     * @param actualArgs the actual type arguments passed to the offspring class
     * @return actual generic type arguments, must match the type parameters of the offspring class. If omitted, the
     * type parameters will be used instead.
     */
    public static <T> Type[] resolveActualTypeArgs(Class<? extends T> offspring, Class<T> base, Type... actualArgs) {

        assert offspring != null;
        assert base != null;
        assert actualArgs.length == 0 || actualArgs.length == offspring.getTypeParameters().length;

        //  If actual types are omitted, the type parameters will be used instead.
        if (actualArgs.length == 0) {
            actualArgs = offspring.getTypeParameters();
        }
        // map type parameters into the actual types
        Map<String, Type> typeVariables = new HashMap<String, Type>();
        for (int i = 0; i < actualArgs.length; i++) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) offspring.getTypeParameters()[i];
            typeVariables.put(typeVariable.getName(), actualArgs[i]);
        }

        // Find direct ancestors (superclass, interfaces)
        List<Type> ancestors = new LinkedList<Type>();
        if (offspring.getGenericSuperclass() != null) {
            ancestors.add(offspring.getGenericSuperclass());
        }
        for (Type t : offspring.getGenericInterfaces()) {
            ancestors.add(t);
        }

        // Recurse into ancestors (superclass, interfaces)
        for (Type type : ancestors) {
            if (type instanceof Class<?>) {
                // ancestor is non-parameterized. Recurse only if it matches the base class.
                Class<?> ancestorClass = (Class<?>) type;
                if (base.isAssignableFrom(ancestorClass)) {
                    Type[] result = resolveActualTypeArgs((Class<? extends T>) ancestorClass, base);
                    if (result != null) {
                        return result;
                    }
                }
            }
            if (type instanceof ParameterizedType) {
                // ancestor is parameterized. Recurse only if the raw type matches the base class.
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class<?>) {
                    Class<?> rawTypeClass = (Class<?>) rawType;
                    if (base.isAssignableFrom(rawTypeClass)) {

                        // loop through all type arguments and replace type variables with the actually known types
                        List<Type> resolvedTypes = new LinkedList<Type>();
                        for (Type t : parameterizedType.getActualTypeArguments()) {
                            if (t instanceof TypeVariable<?>) {
                                Type resolvedType = typeVariables.get(((TypeVariable<?>) t).getName());
                                resolvedTypes.add(resolvedType != null ? resolvedType : t);
                            } else {
                                resolvedTypes.add(t);
                            }
                        }

                        Type[] result = resolveActualTypeArgs((Class<? extends T>) rawTypeClass, base, resolvedTypes.toArray(new Type[]{}));
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }

        // we have a result if we reached the base class.
        return offspring.equals(base) ? actualArgs : null;
    }

    private MessageBodyWriter<Object> getWriter(Type type) {
        if (!type.equals(ByteBuffer.class)) {
            return Optional.ofNullable(messageBodyWorkers.getMessageBodyWriter(Classes.getClass(type), type, new Annotation[0], MediaType.WILDCARD_TYPE))
                    .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyWriter for " + type));
        } else {
            return null;
        }
    }

    private MessageBodyReader<Object> getReader(Type type) {
        if (!type.equals(ByteBuffer.class)) {
            return Optional.ofNullable(messageBodyWorkers.getMessageBodyReader(Classes.getClass(type), type, new Annotation[0], MediaType.WILDCARD_TYPE))
                    .orElseThrow(() -> new IllegalStateException("Could not find MessageBodyReader for " + type));
        } else {
            return null;
        }
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
