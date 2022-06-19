package com.exoreaction.reactiveservices.service.neo4jprojections.eventstore;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.reactiveservices.service.neo4jprojections.ProjectionListener;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.util.Listeners;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Optional;

class EventStoreSubscriber
        implements ReactiveEventStreams.Subscriber<ArrayNode> {

    private Disruptor<Event<ArrayNode>> disruptor;
    private Optional<ObjectNode> selfParameters;
    private EventStoreParameters sourceParameters;
    private GraphDatabaseService graphDatabaseService;
    private Listeners<ProjectionListener> listeners;

    public EventStoreSubscriber(Optional<ObjectNode> selfParameters,
                                EventStoreParameters sourceParameters,
                                GraphDatabaseService graphDatabaseService,
                                Listeners<ProjectionListener> listeners) {

        this.selfParameters = selfParameters;
        this.sourceParameters = sourceParameters;
        this.graphDatabaseService = graphDatabaseService;
        this.listeners = listeners;
    }

    @Override
    public EventSink<Event<ArrayNode>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("Neo4jDomainEventsDisruptorIn-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new Neo4jEventStoreEventHandler(graphDatabaseService, subscription, sourceParameters, listeners));
        disruptor.start();
        subscription.request(4096);
        return disruptor.getRingBuffer();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
