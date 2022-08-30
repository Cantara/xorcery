package com.exoreaction.xorcery.service.neo4jprojections.eventstore;

import com.exoreaction.xorcery.util.Listeners;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.concurrent.CompletableFuture;

class EventStoreSubscriber
        implements ReactiveEventStreams.Subscriber<ArrayNode> {

    private Disruptor<Event<ArrayNode>> disruptor;
    private EventStoreParameters sourceParameters;
    private GraphDatabaseService graphDatabaseService;
    private Listeners<ProjectionListener> listeners;
    private long lastRevision;
    private CompletableFuture<Void> isLive;
    private Configuration consumerConfiuration;

    public EventStoreSubscriber(Configuration consumerConfiuration,
                                EventStoreParameters sourceParameters,
                                GraphDatabaseService graphDatabaseService,
                                Listeners<ProjectionListener> listeners,
                                long lastRevision,
                                CompletableFuture<Void> isLive) {
        this.consumerConfiuration = consumerConfiuration;
        this.sourceParameters = sourceParameters;
        this.graphDatabaseService = graphDatabaseService;
        this.listeners = listeners;
        this.lastRevision = lastRevision;
        this.isLive = isLive;
    }

    @Override
    public EventSink<Event<ArrayNode>> onSubscribe(ReactiveEventStreams.Subscription subscription, Configuration configuration) {
        disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("Neo4jEventStoreProjection-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new Neo4jEventStoreEventHandler(graphDatabaseService, subscription, sourceParameters, consumerConfiuration, listeners, lastRevision, isLive));
        disruptor.start();
        subscription.request(4096);
        return disruptor.getRingBuffer();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
