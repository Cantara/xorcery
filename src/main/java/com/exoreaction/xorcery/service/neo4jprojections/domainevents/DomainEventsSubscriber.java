package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.util.Listeners;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.function.Function;

class DomainEventsSubscriber
        implements ReactiveEventStreams.Subscriber<EventWithResult<ArrayNode, Metadata>> {

    private final Function<ReactiveEventStreams.Subscription, Neo4jDomainEventEventHandler> handlerFactory;
    private Disruptor<Event<EventWithResult<ArrayNode, Metadata>>> disruptor;

    public DomainEventsSubscriber(Function<ReactiveEventStreams.Subscription, Neo4jDomainEventEventHandler> handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    @Override
    public EventSink<Event<EventWithResult<ArrayNode, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription, Configuration configuration) {
        disruptor = new Disruptor<>(Event::new, 1024, new NamedThreadFactory("Neo4jDomainEventsProjection-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());

        disruptor.handleEventsWith(handlerFactory.apply(subscription));
        disruptor.start();
        subscription.request(disruptor.getBufferSize());
        return disruptor.getRingBuffer();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
