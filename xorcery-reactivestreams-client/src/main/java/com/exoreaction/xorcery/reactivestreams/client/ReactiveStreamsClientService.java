/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.client;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.reactivestreams.common.LocalStreamFactories;
import com.exoreaction.xorcery.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.lang.reflect.Type;
import java.net.URI;
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

    private final ReactiveStreamsClientConfiguration clientConfiguration;
    private DnsLookup dnsLookup;
    private MetricRegistry metricRegistry;
    private final Supplier<LocalStreamFactories> reactiveStreamsServerServiceProvider;
    private final WebSocketClient webSocketClient;

    private final List<CompletableFuture<Void>> activeSubscribeProcesses = new CopyOnWriteArrayList<>();
    private final List<CompletableFuture<Void>> activePublishProcesses = new CopyOnWriteArrayList<>();

    public ReactiveStreamsClientService(Configuration configuration,
                                        MessageWorkers messageWorkers,
                                        HttpClient httpClient,
                                        DnsLookupService dnsLookup,
                                        MetricRegistry metricRegistry,
                                        Supplier<LocalStreamFactories> localStreamFactoriesProvider,
                                        Logger logger) throws Exception {
        super(messageWorkers, logger);
        this.dnsLookup = dnsLookup;
        this.metricRegistry = metricRegistry;
        this.reactiveStreamsServerServiceProvider = localStreamFactoriesProvider;

        clientConfiguration = new ReactiveStreamsClientConfiguration(configuration.getConfiguration("reactivestreams.client"));

        WebSocketClient webSocketClient = new WebSocketClient(httpClient);
        webSocketClient.setStopAtShutdown(false);
        webSocketClient.setIdleTimeout(clientConfiguration.getIdleTimeout());
        webSocketClient.setConnectTimeout(clientConfiguration.getConnectTimeout().toMillis());
        webSocketClient.setAutoFragment(clientConfiguration.isAutoFragment());
        webSocketClient.setMaxTextMessageSize(clientConfiguration.getMaxTextMessageSize());
        webSocketClient.setMaxBinaryMessageSize(clientConfiguration.getMaxBinaryMessageSize());
        webSocketClient.setMaxFrameSize(clientConfiguration.getMaxFrameSize());
        webSocketClient.setInputBufferSize(clientConfiguration.getInputBufferSize());
        webSocketClient.setOutputBufferSize(clientConfiguration.getOutputBufferSize());
        webSocketClient.start();
        this.webSocketClient = webSocketClient;
    }

    @Override
    public CompletableFuture<Void> publish(URI serverUri, String streamName, Supplier<Configuration> subscriberServerConfiguration,
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

        if (serverUri == null) {
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
        } else {
            if (!("ws".equals(serverUri.getScheme()) || "wss".equals(serverUri.getScheme()) || "srv".equals(serverUri.getScheme()))) {
                result.completeExceptionally(new IllegalArgumentException("URI scheme " + serverUri.getScheme() + " not supported. Must be one of 'ws', 'wss', or 'srv'"));
            }

        }

        Type type = resolveActualTypeArgs(publisherType, Flow.Publisher.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);

        MessageWriter<Object> eventWriter = getWriter(eventType);
        MessageReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

        // Start publishing process
        if (resultReader != null) {
            switch (clientConfiguration.getStrategy()) {
                case standard -> {
                    new PublishWithResultReactiveStreamStandard(
                            serverUri, streamName,
                            publisherClientConfiguration,
                            dnsLookup,
                            webSocketClient,
                            (Flow.Publisher<Object>) publisher,
                            eventWriter,
                            resultReader,
                            subscriberServerConfiguration,
                            byteBufferPool,
                            metricRegistry,
                            result);
                }
                case disruptor -> {
                    new PublishWithResultReactiveStreamDisruptor(
                            serverUri.getScheme(), serverUri.getAuthority(), streamName,
                            publisherClientConfiguration,
                            dnsLookup,
                            webSocketClient,
                            (Flow.Publisher<Object>) publisher,
                            eventWriter,
                            resultReader,
                            subscriberServerConfiguration,
                            byteBufferPool,
                            metricRegistry,
                            result);
                }
            }
        } else {
            switch (clientConfiguration.getStrategy()) {
                case standard -> {
                    new PublishReactiveStreamStandard(serverUri,
                            streamName,
                            publisherClientConfiguration,
                            dnsLookup,
                            webSocketClient,
                            (Flow.Publisher<Object>) publisher,
                            eventWriter,
                            subscriberServerConfiguration,
                            byteBufferPool,
                            metricRegistry,
                            result);
                }
                case disruptor -> {
                    new PublishReactiveStreamDisruptor(serverUri.getScheme(), serverUri.getAuthority(),
                            streamName,
                            publisherClientConfiguration,
                            dnsLookup,
                            webSocketClient,
                            (Flow.Publisher<Object>) publisher,
                            eventWriter,
                            subscriberServerConfiguration,
                            byteBufferPool,
                            metricRegistry,
                            result);
                }
            }
        }
        return result;
    }

    @Override
    public CompletableFuture<Void> subscribe(URI serverUri, String streamName, Supplier<Configuration> publisherConfiguration,
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

        if (serverUri == null) {
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
        } else {
            if (!("ws".equals(serverUri.getScheme()) || "wss".equals(serverUri.getScheme()) || "srv".equals(serverUri.getScheme()))) {
                result.completeExceptionally(new IllegalArgumentException("URI scheme " + serverUri.getScheme() + " not supported. Must be one of 'ws', 'wss', or 'srv'"));
            }
        }

        Type type = resolveActualTypeArgs(subscriberType, Flow.Subscriber.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);

        MessageReader<Object> eventReader = getReader(eventType);
        MessageWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

        // Subscriber wrapper so we can track active subscriptions
//        subscriber = new SubscriberTracker((Flow.Subscriber<Object>) subscriber, result);

        // Start subscription process
        if (resultWriter != null) {
            new SubscribeWithResultReactiveStream(
                    serverUri, streamName,
                    subscriberClientConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Subscriber<Object>) subscriber,
                    eventReader,
                    resultWriter,
                    publisherConfiguration,
                    byteBufferPool,
                    metricRegistry,
                    LogManager.getLogger(SubscribeReactiveStream.class),
                    result);

        } else {
            new SubscribeReactiveStream(
                    serverUri, streamName,
                    subscriberClientConfiguration,
                    dnsLookup,
                    webSocketClient,
                    (Flow.Subscriber<Object>) subscriber,
                    eventReader,
                    publisherConfiguration,
                    byteBufferPool,
                    metricRegistry,
                    LogManager.getLogger(SubscribeReactiveStream.class),
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
