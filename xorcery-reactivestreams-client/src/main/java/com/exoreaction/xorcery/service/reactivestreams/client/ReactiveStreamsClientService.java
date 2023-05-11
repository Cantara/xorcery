package com.exoreaction.xorcery.service.reactivestreams.client;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.DefaultsConfiguration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.api.ClientConfiguration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.common.LocalStreamFactories;
import com.exoreaction.xorcery.service.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ReactiveStreamsClientService
        extends ReactiveStreamsAbstractService
        implements ReactiveStreamsClient {

    private DnsLookup dnsLookup;
    private MetricRegistry metricRegistry;
    private final Supplier<LocalStreamFactories> reactiveStreamsServerServiceProvider;
    private final String defaultScheme;
    private final WebSocketClient webSocketClient;

    private final List<CompletableFuture<Void>> activeSubscribeProcesses = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<Void>> activePublishProcesses = new CopyOnWriteArrayList<>();

    public ReactiveStreamsClientService(Configuration configuration,
                                        MessageWorkers messageWorkers,
                                        HttpClient httpClient,
                                        DnsLookupService dnsLookup,
                                        MetricRegistry metricRegistry,
                                        Supplier<LocalStreamFactories> localStreamFactoriesProvider) throws Exception {
        super(messageWorkers);
        this.dnsLookup = dnsLookup;
        this.metricRegistry = metricRegistry;
        this.reactiveStreamsServerServiceProvider = localStreamFactoriesProvider;
        this.defaultScheme = configuration.getString("reactivestreams.client.scheme").orElseThrow();

        WebSocketClient webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.setIdleTimeout(new DefaultsConfiguration(configuration.getConfiguration("defaults")).getIdleTimeout());
        webSocketClient.start();
        this.webSocketClient = webSocketClient;
    }

    @Override
    public CompletableFuture<Void> publish(String authority, String streamName, Supplier<Configuration> subscriberServerConfiguration,
                                           Flow.Publisher<?> publisher, Class<? extends Flow.Publisher<?>> publisherType, ClientConfiguration publisherClientConfiguration) {

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
            LocalStreamFactories.WrappedSubscriberFactory subscriberFactory = reactiveStreamsServerServiceProvider.get().getSubscriberFactory(streamName);

            if (subscriberFactory != null) {
                // Local
                Flow.Subscriber<Object> subscriber = subscriberFactory.factory().apply(subscriberServerConfiguration.get());
                Type subscriberEventType = getEventType(resolveActualTypeArgs(subscriberFactory.subscriberType(), Flow.Subscriber.class)[0]);
                Type publisherEventType = getEventType(resolveActualTypeArgs(publisherType, Flow.Publisher.class)[0]);

                if (!subscriberEventType.equals(publisherEventType)) {
                    // Do type conversion
                    MessageWriter<Object> writer = getWriter(publisherEventType);
                    MessageReader<Object> reader = getReader(subscriberEventType);
                    subscriber = new SubscriberConverter(subscriber, writer, reader);
                }

                subscriber = new SubscribeResultHandler(subscriber, result);

                publisher.subscribe(subscriber);
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
        if (resultReader != null) {
            new PublishWithResultReactiveStream(
                    defaultScheme, authority, streamName,
                    publisherClientConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Publisher<Object>) publisher,
                    eventWriter,
                    resultReader,
                    subscriberServerConfiguration,
                    timer,
                    byteBufferPool,
                    metricRegistry,
                    result);
        } else {
            new PublishReactiveStream(
                    defaultScheme, authority, streamName,
                    publisherClientConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Publisher<Object>) publisher,
                    eventWriter,
                    subscriberServerConfiguration,
                    timer,
                    byteBufferPool,
                    metricRegistry,
                    result);

        }
        return result;
    }

    @Override
    public CompletableFuture<Void> subscribe(String authority, String streamName, Supplier<Configuration> publisherConfiguration,
                                             Flow.Subscriber<?> subscriber, Class<? extends Flow.Subscriber<?>> subscriberType, ClientConfiguration subscriberClientConfiguration) {
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
            LocalStreamFactories.WrappedPublisherFactory publisherFactory = reactiveStreamsServerServiceProvider.get().getPublisherFactory(streamName);

            if (publisherFactory != null) {
                // Local
                Flow.Publisher<Object> publisher = publisherFactory.factory().apply(publisherConfiguration.get());
                Type subscriberEventType = getEventType(resolveActualTypeArgs(subscriberType, Flow.Subscriber.class)[0]);
                Type publisherEventType = getEventType(resolveActualTypeArgs(publisherFactory.publisherType(), Flow.Publisher.class)[0]);

                if (!subscriberEventType.equals(publisherEventType)) {
                    // Do type conversion
                    MessageWriter<Object> writer = getWriter(publisherEventType);
                    MessageReader<Object> reader = getReader(subscriberEventType);
                    subscriber = new SubscriberConverter((Flow.Subscriber<Object>) subscriber, writer, reader);
                }

                subscriber = new SubscribeResultHandler((Flow.Subscriber<Object>) subscriber, result);

                publisher.subscribe((Flow.Subscriber<Object>) subscriber);
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
        if (resultWriter != null) {
            new SubscribeWithResultReactiveStream(
                    defaultScheme, authority, streamName,
                    subscriberClientConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Subscriber<Object>) subscriber,
                    eventReader,
                    resultWriter,
                    publisherConfiguration,
                    timer,
                    byteBufferPool,
                    metricRegistry,
                    result);

        } else {
            new SubscribeReactiveStream(
                    defaultScheme, authority, streamName,
                    subscriberClientConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Subscriber<Object>) subscriber,
                    eventReader,
                    publisherConfiguration,
                    timer,
                    byteBufferPool,
                    metricRegistry,
                    result);
        }
        return result;
    }

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
