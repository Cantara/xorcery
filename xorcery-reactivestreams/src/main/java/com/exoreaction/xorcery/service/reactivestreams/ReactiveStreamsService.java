package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.PublisherWebSocketServlet;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.SubscriberWebSocketServlet;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.util.Classes;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsService.SERVICE_TYPE;

@Service(name = SERVICE_TYPE)
@ContractsProvided({ReactiveStreams.class, ReactiveStreamsClient.class})
public class ReactiveStreamsService
        implements ReactiveStreams, PreDestroy {

    private static final Logger logger = LogManager.getLogger(ReactiveStreamsService.class);
    public static final String SERVICE_TYPE = "reactivestreams";

    // Magic bytes for sending exceptions
    public static final byte[] XOR = "XOR".getBytes(StandardCharsets.UTF_8);

    private final boolean allowLocal;
    private final ServletContextHandler servletContextHandler;
    private final WebSocketClient webSocketClient;
    private final StandardConfiguration configuration;
    private final MessageWorkers messageWorkers;
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
                                  HttpClient httpClient,
                                  Configuration configuration,
                                  MessageWorkers messageWorkers) throws Exception {
        this.servletContextHandler = servletContextHandler;

        this.configuration = () -> configuration;
        this.messageWorkers = messageWorkers;
        this.objectMapper = new ObjectMapper();
        this.allowLocal = configuration.getBoolean("reactivestreams.allowlocal").orElse(true);
        this.timer = new Timer();

        WebSocketClient webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));
        webSocketClient.start();
        this.webSocketClient = webSocketClient;

        byteBufferPool = new ArrayByteBufferPool();
    }

    @Override
    public void preDestroy() {
        logger.info("Stop reactive streams");

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
    public CompletableFuture<Void> publisher(String publisherWebsocketPath,
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
        MessageWriter<Object> eventWriter = getWriter(eventType);
        MessageReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

        PublisherWebSocketServlet servlet = new PublisherWebSocketServlet(
                publisherWebsocketPath,
                config -> new PublisherTracker((Flow.Publisher<Object>) publisherFactory.apply(config)),
                eventWriter,
                resultReader,
                configuration.context(),
                objectMapper,
                byteBufferPool);

        servletContextHandler.addServlet(new ServletHolder(servlet), publisherWebsocketPath);
        logger.info("Added publisher websocket for " + publisherWebsocketPath);

        // TODO Shutdown the above on completable cancel
        return new CompletableFuture<>();
    }

    @Override
    public CompletableFuture<Void> subscriber(String subscriberWebsocketPath, Function<Configuration, Flow.Subscriber<?>> subscriberFactory, Class<? extends Flow.Subscriber<?>> subscriberType) {
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
        MessageReader<Object> eventReader = getReader(eventType);
        MessageWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

        SubscriberWebSocketServlet servlet = new SubscriberWebSocketServlet(
                subscriberWebsocketPath,
                config -> new SubscriberTracker((Flow.Subscriber<Object>) subscriberFactory.apply(config)),
                resultWriter,
                eventReader,
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
    public CompletableFuture<Void> publish(URI subscriberWebsocketUri, Configuration subscriberConfiguration, Flow.Publisher<?> publisher, Class<? extends Flow.Publisher<?>> publisherType) {

        CompletableFuture<Void> result = new CompletableFuture<>();

        if (!(subscriberWebsocketUri.getScheme().equals("ws") || subscriberWebsocketUri.getScheme().equals("wss")))
        {
            result.completeExceptionally(new IllegalArgumentException("Unsupported URL scheme:"+subscriberWebsocketUri.toASCIIString()));
            return result;
        }

        if (publisherType == null)
            publisherType = (Class<? extends Flow.Publisher<?>>) publisher.getClass();



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

        MessageWriter<Object> eventWriter = getWriter(eventType);
        MessageReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

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
                subscriberWebsocketUri,
                subscriberConfiguration,
                (Flow.Publisher<Object>) publisher,
                result).start();
        return result;
    }

    @Override
    public CompletableFuture<Void> subscribe(URI publisherWebsocketUri, Configuration publisherConfiguration, Flow.Subscriber<?> subscriber, Class<? extends Flow.Subscriber<?>> subscriberType) {
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

        MessageReader<Object> eventReader = getReader(eventType);
        MessageWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

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

    private MessageWriter<Object> getWriter(Type type) {
        return Optional.ofNullable(messageWorkers.newWriter(Classes.getClass(type), type, MediaType.WILDCARD_TYPE.toString()))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + type));
    }

    private MessageReader<Object> getReader(Type type) {
        return Optional.ofNullable(messageWorkers.newReader(Classes.getClass(type), type, MediaType.WILDCARD_TYPE.toString()))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageReader for " + type));
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
