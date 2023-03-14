package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ProjectionSubscriber
        implements Flow.Subscriber<WithMetadata<ArrayNode>> {

    private final Function<Flow.Subscription, Neo4jProjectionEventHandler> handlerFactory;
    private Disruptor<WithMetadata<ArrayNode>> disruptor;
    private final DisruptorConfiguration disruptorConfiguration;

    public ProjectionSubscriber(Function<Flow.Subscription, Neo4jProjectionEventHandler> handlerFactory, DisruptorConfiguration disruptorConfiguration) {
        this.disruptorConfiguration = disruptorConfiguration;
        this.handlerFactory = handlerFactory;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        disruptor = new Disruptor<>(WithMetadata::new, disruptorConfiguration.getSize(), new NamedThreadFactory("Neo4jProjection-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());

        disruptor.handleEventsWith(handlerFactory.apply(subscription));
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
        try {
            disruptor.shutdown(disruptorConfiguration.getShutdownTimeout(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LogManager.getLogger(getClass()).warn("Could not shutdown disruptor within timeout", e);
        }
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
