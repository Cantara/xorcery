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
package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.domainevents.api.CommandEvents;
import com.exoreaction.xorcery.domainevents.api.DomainEvent;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjectionPreProcessor;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.IterableProvider;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ProjectionSubscriber
        implements Subscriber<WithMetadata<ArrayNode>> {

    private final Function<Subscription, Neo4jProjectionEventHandler> handlerFactory;
    private final Function<Subscription, ExceptionHandler<CommandEvents>> exceptionHandlerFactory;
    private final ObjectReader domainEventsReader;
    private Disruptor<CommandEvents> disruptor;
    private final IterableProvider<Neo4jEventProjectionPreProcessor> preProcessors;
    private final DisruptorConfiguration disruptorConfiguration;
    private Subscription subscription;

    public ProjectionSubscriber(Function<Subscription, Neo4jProjectionEventHandler> handlerFactory,
                                IterableProvider<Neo4jEventProjectionPreProcessor> preProcessors,
                                DisruptorConfiguration disruptorConfiguration,
                                Function<Subscription, ExceptionHandler<CommandEvents>> exceptionHandlerFactory) {
        this.preProcessors = preProcessors;
        this.disruptorConfiguration = disruptorConfiguration;
        this.handlerFactory = handlerFactory;
        this.exceptionHandlerFactory = exceptionHandlerFactory;
        JsonMapper mapper = new JsonMapper();
        domainEventsReader = mapper.readerFor(mapper.getTypeFactory().constructCollectionType(List.class, DomainEvent.class));
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        disruptor = new Disruptor<>(CommandEvents::new, disruptorConfiguration.getSize(), new NamedThreadFactory("Neo4jProjection-"),
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        EventHandlerGroup<CommandEvents> eventHandlerGroup = null;
        for (Neo4jEventProjectionPreProcessor preProcessor : preProcessors) {
            eventHandlerGroup = (eventHandlerGroup == null) ?
                    disruptor.handleEventsWith(new PreProcessorEventHandler(preProcessor)):
                    eventHandlerGroup.handleEventsWith(new PreProcessorEventHandler(preProcessor));
        }

        Neo4jProjectionEventHandler neo4jProjectionEventHandler = handlerFactory.apply(subscription);
        BatchRewindStrategy rewindStrategy = new EventuallyGiveUpBatchRewindStrategy(10);
        if (eventHandlerGroup == null)
        {
            disruptor.handleEventsWith(rewindStrategy, neo4jProjectionEventHandler);
        } else
        {
            eventHandlerGroup.handleEventsWith(rewindStrategy, neo4jProjectionEventHandler);
        }

        disruptor.setDefaultExceptionHandler(exceptionHandlerFactory.apply(subscription));
        disruptor.start();
        subscription.request(disruptor.getBufferSize());
    }

    @Override
    public void onNext(WithMetadata<ArrayNode> item) {

        try {
            List<DomainEvent> domainEvents = domainEventsReader.readValue(item.event());

            disruptor.publishEvent((e, seq, md, events) ->
            {
                e.set(md, events);
            }, item.metadata(), domainEvents);
        } catch (IOException e) {
            LogManager.getLogger().error("Projection failed", e);
            subscription.cancel();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (disruptor != null)
        {
            try {
                disruptor.shutdown(disruptorConfiguration.getShutdownTimeout(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LogManager.getLogger(getClass()).warn("Could not shutdown disruptor within timeout", e);
            }
        }
        LogManager.getLogger().error("Projection failed", throwable);
    }

    @Override
    public void onComplete() {
        try {
            disruptor.shutdown(disruptorConfiguration.getShutdownTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LogManager.getLogger(getClass()).warn("Could not shutdown disruptor within timeout", e);
        }
    }
}
