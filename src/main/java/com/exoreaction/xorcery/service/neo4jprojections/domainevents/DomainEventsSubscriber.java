package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.Subscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.Subscription;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.Flow;
import java.util.function.Function;

class DomainEventsSubscriber
        implements Flow.Subscriber<WithResult<WithMetadata<ArrayNode>, Metadata>> {

    private final Function<Flow.Subscription, Neo4jDomainEventEventHandler> handlerFactory;
    private Disruptor<WithResult<WithMetadata<ArrayNode>, Metadata>> disruptor;

    public DomainEventsSubscriber(Function<Flow.Subscription, Neo4jDomainEventEventHandler> handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        disruptor = new Disruptor<>(WithResult::new, 1024, new NamedThreadFactory("Neo4jDomainEventsProjection-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());

        disruptor.handleEventsWith(handlerFactory.apply(subscription));
        disruptor.start();
        subscription.request(disruptor.getBufferSize());
    }

    @Override
    public void onNext(WithResult<WithMetadata<ArrayNode>, Metadata> item) {

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
