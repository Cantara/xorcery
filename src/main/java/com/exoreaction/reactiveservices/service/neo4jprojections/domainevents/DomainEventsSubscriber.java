package com.exoreaction.reactiveservices.service.neo4jprojections.domainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Optional;

class DomainEventsSubscriber
        implements ReactiveEventStreams.Subscriber<EventWithResult<ArrayNode, Metadata>> {

    private Disruptor<Event<EventWithResult<ArrayNode, Metadata>>> disruptor;
    private Optional<ObjectNode> selfParameters;
    private Optional<ObjectNode> sourceParameters;
    private GraphDatabaseService graphDatabaseService;

    public DomainEventsSubscriber(Optional<ObjectNode> selfParameters, Optional<ObjectNode> sourceParameters, GraphDatabaseService graphDatabaseService) {

        this.selfParameters = selfParameters;
        this.sourceParameters = sourceParameters;
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public EventSink<Event<EventWithResult<ArrayNode, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("Neo4jDomainEventsDisruptorIn-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new Neo4jDomainEventEventHandler(graphDatabaseService, subscription));
        disruptor.start();
        subscription.request(4096);
        return disruptor.getRingBuffer();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
