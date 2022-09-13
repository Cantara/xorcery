package com.exoreaction.xorcery.service.opensearch.streams;

import com.exoreaction.xorcery.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.xorcery.service.opensearch.api.IndexCommit;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class OpenSearchCommitPublisher
        implements Flow.Publisher<WithMetadata<IndexCommit>>, Consumer<WithMetadata<IndexCommit>> {
    BroadcastEventHandler<WithMetadata<IndexCommit>> broadcastEventHandler = new BroadcastEventHandler<>(true);

    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<IndexCommit>> subscriber) {
        subscriber.onSubscribe(broadcastEventHandler.add(subscriber));
    }

    @Override
    public void accept(WithMetadata<IndexCommit> indexCommitWithMetadata) {
        // TODO Replace with disruptor
        try {
            broadcastEventHandler.onEvent(indexCommitWithMetadata, 0, true);
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Error publishing index commit", e);
        }
    }
}
