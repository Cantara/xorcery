package com.exoreaction.xorcery.service.neo4jprojections.eventstore;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.util.Listeners;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

class EventStoreSubscriber
        implements Flow.Subscriber<WithMetadata<ArrayNode>> {

    private Disruptor<WithMetadata<ArrayNode>> disruptor;
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
    public void onSubscribe(Flow.Subscription subscription) {
        disruptor = new Disruptor<>(WithMetadata::new, 4096, new NamedThreadFactory("Neo4jEventStoreProjection-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new Neo4jEventStoreEventHandler(graphDatabaseService, subscription, sourceParameters, consumerConfiuration, listeners, lastRevision, isLive));
        disruptor.start();
        subscription.request(4096);
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
