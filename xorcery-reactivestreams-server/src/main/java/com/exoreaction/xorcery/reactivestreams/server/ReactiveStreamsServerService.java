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
package com.exoreaction.xorcery.reactivestreams.server;

import com.exoreaction.xorcery.concurrent.CompletableFutures;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.lang.Classes;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.util.*;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.server.*;
import org.glassfish.hk2.runlevel.ChangeableRunLevelFuture;
import org.glassfish.hk2.runlevel.ProgressStartedListener;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Service(name = "reactivestreams.server")
@ContractsProvided({ReactiveStreamsServer.class, ReactiveStreamsServerService.class, ProgressStartedListener.class, LocalStreamFactories.class})
@RunLevel(6)
public class ReactiveStreamsServerService
        extends ReactiveStreamsAbstractService
        implements ReactiveStreamsServer,
        ProgressStartedListener,
        LocalStreamFactories {
    private final Logger logger;
    private final LoggerContext loggerContext;
    private final ActivePublisherSubscriptions activePublisherSubscriptions;
    private final ActiveSubscriberSubscriptions activeSubscriberSubscriptions;
    private final ArrayByteBufferPool byteBufferPool;

    private final Map<String, Supplier<Object>> publisherEndpointFactories = new ConcurrentHashMap<>();
    private final Map<String, WrappedPublisherFactory> publisherLocalFactories = new ConcurrentHashMap<>();
    private final Map<String, Supplier<Object>> subscriberEndpointFactories = new ConcurrentHashMap<>();
    private final Map<String, WrappedSubscriberFactory> subscriberLocalFactories = new ConcurrentHashMap<>();

    private final OpenTelemetry openTelemetry;

    // Currently active subscriptions
    // These need to cancel on shutdown
    protected final List<FutureProcessor<Object>> activeSubscriptions = new CopyOnWriteArrayList<>();

    // Currently active publishers and subscribers
    // These need to cancel on shutdown
    protected final List<CompletableFuture<Object>> activePublisherAndSubscribers = new CopyOnWriteArrayList<>();

    @Inject
    public ReactiveStreamsServerService(Configuration configuration,
                                        OpenTelemetry openTelemetry,
                                        MessageWorkers messageWorkers,
                                        WebSocketUpgradeHandler webSocketUpgradeHandler,
                                        Logger logger,
                                        LoggerContext loggerContext,
                                        ActivePublisherSubscriptions activePublisherSubscriptions,
                                        ActiveSubscriberSubscriptions activeSubscriberSubscriptions) {
        super(messageWorkers);
        this.openTelemetry = openTelemetry;
        this.logger = logger;
        this.byteBufferPool = new ArrayByteBufferPool();

        ReactiveStreamsServerConfiguration reactiveStreamsServerConfiguration = new ReactiveStreamsServerConfiguration(configuration.getConfiguration("reactivestreams.server"));
        this.loggerContext = loggerContext;
        this.activePublisherSubscriptions = activePublisherSubscriptions;
        this.activeSubscriberSubscriptions = activeSubscriberSubscriptions;

        ServerWebSocketContainer container = webSocketUpgradeHandler.getServerWebSocketContainer();
        container.addMapping("/streams/publishers/*", new PublisherWebSocketCreator());
        container.addMapping("/streams/subscribers/*", new SubscriberWebSocketCreator());

    }

    public CompletableFuture<Void> publisher(String streamName,
                                             final Function<Configuration, ? extends Publisher<?>> publisherFactory,
                                             Class<? extends Publisher<?>> publisherType) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        Type type = Classes.resolveActualTypeArgs(publisherType, Publisher.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageWriter<Object> eventWriter = getWriter(eventType);
        MessageReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

        Function<Configuration, Publisher<Object>> wrappedPublisherFactory = (config) ->
        {
            Publisher<Object> publisher = (Publisher<Object>) publisherFactory.apply(config);

            CompletableFuture<Void> subscriptionResult = new CompletableFuture<>();
            FutureProcessor<Object> futureProcessor = new FutureProcessor<>(subscriptionResult);
            publisher.subscribe(futureProcessor);
            activeSubscriptions.add(futureProcessor);
            // Cancel this subscription when the publisher itself cancels
            result.whenComplete(CompletableFutures.transfer(subscriptionResult));
            subscriptionResult.whenComplete((r, t) ->activeSubscriptions.remove(futureProcessor));
            return futureProcessor;
        };

        publisherEndpointFactories.put(streamName, () ->
                resultReader == null ?
                        new PublisherSubscriptionReactiveStream(
                                streamName,
                                wrappedPublisherFactory,
                                eventWriter,
                                objectMapper,
                                byteBufferPool,
                                loggerContext.getLogger(PublisherSubscriptionReactiveStream.class),
                                activePublisherSubscriptions,
                                openTelemetry) :
                        new PublisherWithResultSubscriptionReactiveStream(
                                streamName,
                                wrappedPublisherFactory,
                                eventWriter,
                                resultReader,
                                objectMapper,
                                byteBufferPool,
                                loggerContext.getLogger(PublisherWithResultSubscriptionReactiveStream.class),
                                activePublisherSubscriptions,
                                openTelemetry));
        publisherLocalFactories.put(streamName, new WrappedPublisherFactory(wrappedPublisherFactory, publisherType));
        result.whenComplete((r, t) ->
        {
            publisherEndpointFactories.remove(streamName);
            publisherLocalFactories.remove(streamName);
        });

        return result;
    }

    public CompletableFuture<Void> subscriber(String streamName,
                                              Function<Configuration, Subscriber<?>> subscriberFactory,
                                              Class<? extends Subscriber<?>> subscriberType) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        Type type = Classes.resolveActualTypeArgs(subscriberType, Subscriber.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageReader<Object> eventReader = getReader(eventType);
        MessageWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

        Function<Configuration, Subscriber<Object>> wrappedSubscriberFactory = (config) ->
        {
            Subscriber<Object> subscriber = (Subscriber<Object>) subscriberFactory.apply(config);

            CompletableFuture<Void> subscriptionResult = new CompletableFuture<>();
            FutureProcessor<Object> futureProcessor = new FutureProcessor<>(subscriptionResult);
            futureProcessor.subscribe(subscriber);
            activeSubscriptions.add(futureProcessor);
            // Cancel this subscription when the subscriber itself cancels
            result.whenComplete(CompletableFutures.transfer(subscriptionResult));
            subscriptionResult.whenComplete((r, t) -> activeSubscriptions.remove(futureProcessor));
            return futureProcessor;
        };

        subscriberEndpointFactories.put(streamName, () ->
                resultWriter == null ? new SubscriberSubscriptionReactiveStream(
                        streamName,
                        wrappedSubscriberFactory,
                        eventReader,
                        objectMapper,
                        byteBufferPool,
                        openTelemetry,
                        loggerContext.getLogger(SubscriberSubscriptionReactiveStream.class),
                        activeSubscriberSubscriptions) :
                        new SubscriberWithResultSubscriptionReactiveStream(
                                streamName,
                                wrappedSubscriberFactory,
                                eventReader,
                                resultWriter,
                                objectMapper,
                                byteBufferPool,
                                openTelemetry,
                                loggerContext.getLogger(SubscriberWithResultSubscriptionReactiveStream.class),
                                activeSubscriberSubscriptions));
        subscriberLocalFactories.put(streamName, new WrappedSubscriberFactory(wrappedSubscriberFactory, subscriberType));
        result.whenComplete((r, t) ->
        {
            subscriberEndpointFactories.remove(streamName);
            subscriberLocalFactories.remove(streamName);
        });

        return result;
    }


    public WrappedSubscriberFactory getSubscriberFactory(String streamName) {
        return subscriberLocalFactories.get(streamName);
    }

    public WrappedPublisherFactory getPublisherFactory(String streamName) {
        return publisherLocalFactories.get(streamName);
    }

    @Override
    public void onProgressStarting(ChangeableRunLevelFuture currentJob, int currentLevel) {
        if (currentLevel == 19 && currentJob.getProposedLevel() < currentLevel) {
            // Close existing streams
            logger.info("Cancel subscriptions on shutdown");

            // Cancel all subscriptions
            for (FutureProcessor<Object> activeSubscription : activeSubscriptions) {
                activeSubscription.close();
            }

            // Wait for them to finish
            for (FutureProcessor<Object> activePublisherSubscription : activeSubscriptions) {
                try {
                    activePublisherSubscription.getResult().get(10, TimeUnit.SECONDS);
                    logger.debug("Subscription closed");
                } catch (CancellationException e) {
                    // Ignore, this is ok
                } catch (Throwable e) {
                    logger.warn("Could not shutdown subscription", e);
                }
            }

            // Close publisher futures
            for (CompletableFuture<Object> activePublisherAndSubscriber : activePublisherAndSubscribers) {
                activePublisherAndSubscriber.completeExceptionally(new ServerShutdownStreamException(1001, "Shutting down server"));
            }
        }
    }

    private class PublisherWebSocketCreator implements WebSocketCreator {
        @Override
        public Object createWebSocket(ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse, Callback callback) {
            String streamName = serverUpgradeRequest.getHttpURI().getPath().substring( "/streams/publishers/".length());
            return Optional.ofNullable(publisherEndpointFactories.get(streamName)).map(Supplier::get).orElse(null);
        }
    }

    private class SubscriberWebSocketCreator implements WebSocketCreator {
        @Override
        public Object createWebSocket(ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse, Callback callback) {
            String streamName = serverUpgradeRequest.getHttpURI().getPath().substring( "/streams/subscribers/".length());
            return Optional.ofNullable(subscriberEndpointFactories.get(streamName)).map(Supplier::get).orElse(null);
        }
    }
}
