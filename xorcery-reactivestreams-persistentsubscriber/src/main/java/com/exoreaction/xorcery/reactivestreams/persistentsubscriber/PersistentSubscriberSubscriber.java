package com.exoreaction.xorcery.reactivestreams.persistentsubscriber;

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberErrorLog;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PersistentSubscriberSubscriber
        implements Subscriber<WithMetadata<ArrayNode>> {
    private final PersistentSubscriberProcess persistentSubscriberProcess;
    private final PersistentSubscriber persistentSubscriber;
    private final PersistentSubscriberCheckpoint persistentSubscriberCheckpoint;
    private final PersistentSubscriberErrorLog persistentSubscriberErrorLog;
    private final Logger logger;
    private Subscription subscription;

    public PersistentSubscriberSubscriber(
            PersistentSubscriberProcess persistentSubscriberProcess,
            PersistentSubscriber persistentSubscriber,
            PersistentSubscriberCheckpoint persistentSubscriberCheckpoint,
            PersistentSubscriberErrorLog persistentSubscriberErrorLog,
            Logger logger) {

        this.persistentSubscriberProcess = persistentSubscriberProcess;
        this.persistentSubscriber = persistentSubscriber;
        this.persistentSubscriberCheckpoint = persistentSubscriberCheckpoint;
        this.persistentSubscriberErrorLog = persistentSubscriberErrorLog;
        this.logger = logger;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        logger.debug("onSubscribe");
        subscription.request(1024);
    }

    @Override
    public void onNext(WithMetadata<ArrayNode> arrayNodeWithMetadata) {

        long revision = arrayNodeWithMetadata.metadata().getLong("revision").orElseThrow(() -> new IllegalStateException("Metadata does not contain revision"));

        CompletableFuture<Void> future = new CompletableFuture<>();
        persistentSubscriber.handle(arrayNodeWithMetadata, future);

        try {
            try {
                future.orTimeout(10, TimeUnit.SECONDS).join();

                persistentSubscriberCheckpoint.setCheckpoint(revision);
            } catch (Throwable t) {
                persistentSubscriberErrorLog.handle(arrayNodeWithMetadata, t);
            }
        } catch (IOException e) {
            subscription.cancel();
        }

        subscription.request(1);
    }

    @Override
    public void onError(Throwable t) {
        persistentSubscriberProcess.retry();
    }

    @Override
    public void onComplete() {
        logger.info("onComplete");
    }
}
