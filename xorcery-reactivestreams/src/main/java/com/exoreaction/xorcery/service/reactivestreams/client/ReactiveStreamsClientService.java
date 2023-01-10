package com.exoreaction.xorcery.service.reactivestreams.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.server.ReactiveStreamsServerService;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Service(name="reactivestreams.client")
@ContractsProvided({ReactiveStreamsClient.class})
@RunLevel(8)
public class ReactiveStreamsClientService
        extends ReactiveStreamsAbstractService
        implements ReactiveStreamsClient, PreDestroy {

    private DnsLookup dnsLookup;
    private final Provider<ReactiveStreamsServerService> reactiveStreamsServerServiceProvider;
    private final String scheme;
    private final WebSocketClient webSocketClient;

    private final List<CompletableFuture<Void>> activeSubscribeProcesses = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<Void>> activePublishProcesses = new CopyOnWriteArrayList<>();

    @Inject
    public ReactiveStreamsClientService(Configuration configuration,
                                        MessageWorkers messageWorkers,
                                        HttpClient httpClient,
                                        DnsLookup dnsLookup,
                                        Provider<ReactiveStreamsServerService> reactiveStreamsServerServiceProvider) throws Exception {
        super(messageWorkers);
        this.dnsLookup = dnsLookup;
        this.reactiveStreamsServerServiceProvider = reactiveStreamsServerServiceProvider;
        this.scheme = configuration.getString("reactivestreams.client.scheme").orElseThrow();

        WebSocketClient webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.setIdleTimeout(Duration.ofSeconds(configuration.getLong("idle_timeout").orElse(-1L)));
        webSocketClient.start();
        this.webSocketClient = webSocketClient;
    }

    @Override
    public CompletableFuture<Void> publish(String authority, String streamName, Supplier<Configuration> subscriberConfiguration,
                                           Flow.Publisher<?> publisher, Class<? extends Flow.Publisher<?>> publisherType, Configuration publisherConfiguration) {

        CompletableFuture<Void> result = new CompletableFuture<>();

        if (publisherType == null)
            publisherType = (Class<? extends Flow.Publisher<?>>) publisher.getClass();

        // TODO Track the publishing process itself. Need to ensure they are cancelled on shutdown
        result.whenComplete((r, t) ->
        {
            activePublishProcesses.remove(result);
        });
        activePublishProcesses.add(result);

        if (authority == null) {
            Function<Configuration, Flow.Subscriber<?>> subscriberFactory = reactiveStreamsServerServiceProvider.get().getSubscriberFactory(streamName);

            if (subscriberFactory != null) {
                // Local
                publisher.subscribe((Flow.Subscriber<Object>) subscriberFactory.apply(subscriberConfiguration.get()));
                return result;
            } else {
                result.completeExceptionally(new IllegalArgumentException("No such subscriber:" + streamName));
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
        if (resultReader != null)
        {
            new PublishWithResultReactiveStream(
                    scheme, authority, streamName,
                    publisherConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Publisher<Object>) publisher,
                    eventWriter,
                    resultReader,
                    subscriberConfiguration,
                    timer,
                    byteBufferPool,
                    result);
        } else
        {
            new PublishReactiveStream(
                    scheme, authority, streamName,
                    publisherConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Publisher<Object>) publisher,
                    eventWriter,
                    subscriberConfiguration,
                    timer,
                    byteBufferPool,
                    result);

        }
        return result;
    }

    @Override
    public CompletableFuture<Void> subscribe(String authority, String streamName, Supplier<Configuration> publisherConfiguration,
                                             Flow.Subscriber<?> subscriber, Class<? extends Flow.Subscriber<?>> subscriberType, Configuration subscriberConfiguration) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        if (subscriberType == null)
            subscriberType = (Class<? extends Flow.Subscriber<?>>) subscriber.getClass();

        // Track the subscription process itself. Need to ensure they are cancelled on shutdown
        result.whenComplete((r, t) ->
        {
            activeSubscribeProcesses.remove(result);
        });
        activeSubscribeProcesses.add(result);

        if (authority == null) {
            Function<Configuration, ? extends Flow.Publisher<?>> publisher = reactiveStreamsServerServiceProvider.get().getPublisherFactory(streamName);

            if (publisher != null) {
                // Local
                publisher.apply(publisherConfiguration.get())
                        .subscribe((Flow.Subscriber<Object>) subscriber);
                return result;
            } else {
                result.completeExceptionally(new IllegalArgumentException("No such publisher:" + streamName));
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
        if (resultWriter != null)
        {
            new SubscribeWithResultReactiveStream(
                    scheme, authority, streamName,
                    subscriberConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Subscriber<Object>) subscriber,
                    eventReader,
                    resultWriter,
                    publisherConfiguration,
                    timer,
                    byteBufferPool,
                    result);

        } else {
            new SubscribeReactiveStream(
                    scheme, authority, streamName,
                    subscriberConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Subscriber<Object>) subscriber,
                    eventReader,
                    publisherConfiguration,
                    timer,
                    byteBufferPool,
                    result);
        }
        return result;
    }

    @Override
    public void preDestroy() {
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

        super.preDestroy();

        logger.info("Shutdown reactive services client");
        try {
            webSocketClient.stop();
        } catch (Exception e) {
            logger.warn("Could not stop websocket client", e);
        }
    }
}
