package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

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

import java.util.concurrent.CompletionStage;

class DomainEventsSubscriber
        implements ReactiveEventStreams.Subscriber<EventWithResult<ArrayNode, Metadata>> {

    private final Configuration consumerConfiguration;
    private final Configuration sourceConfiguration;
    private Disruptor<Event<EventWithResult<ArrayNode, Metadata>>> disruptor;
    private GraphDatabaseService graphDatabaseService;
    private Listeners<ProjectionListener> listeners;

    public DomainEventsSubscriber(Configuration consumerConfiguration,
                                  Configuration sourceConfiguration,
                                  GraphDatabaseService graphDatabaseService,
                                  Listeners<ProjectionListener> listeners) {
        this.consumerConfiguration = consumerConfiguration;
        this.sourceConfiguration = sourceConfiguration;
        this.graphDatabaseService = graphDatabaseService;
        this.listeners = listeners;
    }

    @Override
    public EventSink<Event<EventWithResult<ArrayNode, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("Neo4jDomainEventsProjection-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new Neo4jDomainEventEventHandler(graphDatabaseService, subscription, sourceConfiguration, consumerConfiguration, listeners));
        disruptor.start();
        subscription.request(4096);
        return disruptor.getRingBuffer();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
