package com.exoreaction.xorcery.service.neo4jprojections.eventstore;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.util.Listeners;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.neo4j.graphdb.GraphDatabaseService;

class EventStoreSubscriber
        implements ReactiveEventStreams.Subscriber<ArrayNode> {

    private Disruptor<Event<ArrayNode>> disruptor;
    private EventStoreParameters sourceParameters;
    private GraphDatabaseService graphDatabaseService;
    private Listeners<ProjectionListener> listeners;
    private Configuration consumerConfiuration;

    public EventStoreSubscriber(Configuration consumerConfiuration,
                                EventStoreParameters sourceParameters,
                                GraphDatabaseService graphDatabaseService,
                                Listeners<ProjectionListener> listeners) {
        this.consumerConfiuration = consumerConfiuration;
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
