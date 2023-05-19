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
package com.exoreaction.xorcery.service.reactivestreams.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.service.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.common.LocalStreamFactories;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.runlevel.*;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
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
    private final Map<String, Supplier<Object>> publisherEndpointFactories = new ConcurrentHashMap<>();
    private final Map<String, WrappedPublisherFactory> publisherLocalFactories = new ConcurrentHashMap<>();
    private final Map<String, Supplier<Object>> subscriberEndpointFactories = new ConcurrentHashMap<>();
    private final Map<String, WrappedSubscriberFactory> subscriberLocalFactories = new ConcurrentHashMap<>();
    private final MetricRegistry metricRegistry;

    @Inject
    public ReactiveStreamsServerService(Configuration configuration,
                                        MetricRegistry metricRegistry,
                                        MessageWorkers messageWorkers,
                                        ServletContextHandler servletContextHandler) {
        super(messageWorkers);
        this.metricRegistry = metricRegistry;

        PublishersReactiveStreamsServlet publishersServlet = new PublishersReactiveStreamsServlet(configuration, streamName ->
        {
            return Optional.ofNullable(publisherEndpointFactories.get(streamName)).map(Supplier::get).orElse(null);
        });
        servletContextHandler.addServlet(new ServletHolder(publishersServlet), "/streams/publishers/*");

        SubscribersReactiveStreamsServlet subscribersServlet = new SubscribersReactiveStreamsServlet(configuration, streamName ->
        {
            return Optional.ofNullable(subscriberEndpointFactories.get(streamName)).map(Supplier::get).orElse(null);
        });
        servletContextHandler.addServlet(new ServletHolder(subscribersServlet), "/streams/subscribers/*");

    }

    public CompletableFuture<Void> publisher(String streamName,
                                             final Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory,
                                             Class<? extends Flow.Publisher<?>> publisherType) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscriptions to this publisher. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            publisherEndpointFactories.remove(streamName);
            publisherLocalFactories.remove(streamName);
        });

        Type type = resolveActualTypeArgs(publisherType, Flow.Publisher.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageWriter<Object> eventWriter = getWriter(eventType);
        MessageReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

        Function<Configuration, Flow.Publisher<Object>> wrappedPublisherFactory = (config) -> new ReactiveStreamsAbstractService.PublisherTracker((Flow.Publisher<Object>) publisherFactory.apply(config));

        publisherEndpointFactories.put(streamName, () ->
        {
            return resultReader == null ? new PublisherReactiveStream(streamName, wrappedPublisherFactory, eventWriter, objectMapper, byteBufferPool) :
                    new PublisherWithResultReactiveStream(streamName, wrappedPublisherFactory, eventWriter, resultReader, objectMapper, byteBufferPool);
        });
        publisherLocalFactories.put(streamName, new WrappedPublisherFactory(wrappedPublisherFactory, publisherType));

        return result;
    }

    public CompletableFuture<Void> subscriber(String streamName,
                                              Function<Configuration, Flow.Subscriber<?>> subscriberFactory,
                                              Class<? extends Flow.Subscriber<?>> subscriberType) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscriptions to this publisher. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            subscriberEndpointFactories.remove(streamName);
            subscriberLocalFactories.remove(streamName);
        });

        Type type = resolveActualTypeArgs(subscriberType, Flow.Subscriber.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageReader<Object> eventReader = getReader(eventType);
        MessageWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

        Function<Configuration, Flow.Subscriber<Object>> wrappedSubscriberFactory = (config) -> new ReactiveStreamsAbstractService.SubscriberTracker((Flow.Subscriber<Object>) subscriberFactory.apply(config));

        subscriberEndpointFactories.put(streamName, () ->
                resultWriter == null ? new SubscriberReactiveStream(streamName, wrappedSubscriberFactory, eventReader, objectMapper, byteBufferPool, timer, metricRegistry) :
                        new SubscriberWithResultReactiveStream(streamName, wrappedSubscriberFactory, eventReader, resultWriter, objectMapper, byteBufferPool, timer, metricRegistry));
        subscriberLocalFactories.put(streamName, new WrappedSubscriberFactory(wrappedSubscriberFactory, subscriberType));
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
        if (currentLevel == 20 && currentJob.getProposedLevel() < currentLevel) {
            // Close existing streams
            logger.info("Cancel subscriptions on shutdown");
            cancelActiveSubscriptions();
        }
    }
}
