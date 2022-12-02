package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.Flow;
import java.util.function.Function;

public class ProjectionSubscriber
        implements Flow.Subscriber<WithMetadata<ArrayNode>> {

    private final Function<Flow.Subscription, Neo4jProjectionEventHandler> handlerFactory;
    private final int ringBufferSize;
    private Disruptor<WithMetadata<ArrayNode>> disruptor;

    public ProjectionSubscriber(Function<Flow.Subscription, Neo4jProjectionEventHandler> handlerFactory, int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
        this.handlerFactory = handlerFactory;
    }

    public ProjectionSubscriber(Function<Flow.Subscription, Neo4jProjectionEventHandler> handlerFactory) {
        this(handlerFactory, 512);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        disruptor = new Disruptor<>(WithMetadata::new, ringBufferSize, new NamedThreadFactory("Neo4jProjection-"),
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
        disruptor.shutdown();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
