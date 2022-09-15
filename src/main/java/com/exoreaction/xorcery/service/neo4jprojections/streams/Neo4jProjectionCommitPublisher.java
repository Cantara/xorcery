package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class Neo4jProjectionCommitPublisher
        implements Flow.Publisher<WithMetadata<ProjectionCommit>>, Consumer<WithMetadata<ProjectionCommit>> {
    BroadcastEventHandler<WithMetadata<ProjectionCommit>> broadcastEventHandler = new BroadcastEventHandler<>(true);

    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<ProjectionCommit>> subscriber) {
        subscriber.onSubscribe(broadcastEventHandler.add(subscriber));
    }

    @Override
    public void accept(WithMetadata<ProjectionCommit> projectionCommitWithMetadata) {
        // TODO Replace with disruptor
        try {
            broadcastEventHandler.onEvent(projectionCommitWithMetadata, 0, true);
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Error publishing projection commit", e);
        }
    }
}
