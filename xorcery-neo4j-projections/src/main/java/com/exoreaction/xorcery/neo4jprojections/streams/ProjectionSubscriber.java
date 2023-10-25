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
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ProjectionSubscriber
        implements Subscriber<WithMetadata<ArrayNode>> {

    private final Function<Subscription, Neo4jProjectionEventHandler> handlerFactory;
    private Disruptor<WithMetadata<ArrayNode>> disruptor;
    private final DisruptorConfiguration disruptorConfiguration;

    public ProjectionSubscriber(Function<Subscription, Neo4jProjectionEventHandler> handlerFactory, DisruptorConfiguration disruptorConfiguration) {
        this.disruptorConfiguration = disruptorConfiguration;
        this.handlerFactory = handlerFactory;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        disruptor = new Disruptor<>(WithMetadata::new, disruptorConfiguration.getSize(), new NamedThreadFactory("Neo4jProjection-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());

        disruptor.handleEventsWith(handlerFactory.apply(subscription));
        disruptor.setDefaultExceptionHandler(new ProjectionExceptionHandler(subscription));
        disruptor.start();
        subscription.request(disruptor.getBufferSize());
    }

    @Override
    public void onNext(WithMetadata<ArrayNode> item) {

        disruptor.publishEvent((e, seq, event) ->
        {
            e.set(event);
        }, item);
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

    private static class ProjectionExceptionHandler implements ExceptionHandler<WithMetadata<ArrayNode>> {
        private final Subscription subscription;

        public ProjectionExceptionHandler(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void handleEventException(Throwable ex, long sequence, WithMetadata<ArrayNode> event) {
            subscription.cancel();
            LogManager.getLogger(getClass()).error("Cancelled subscription", ex);
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            subscription.cancel();
            LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            subscription.cancel();
            LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
        }
    }
}
