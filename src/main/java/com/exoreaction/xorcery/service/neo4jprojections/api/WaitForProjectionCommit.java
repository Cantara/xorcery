package com.exoreaction.xorcery.service.neo4jprojections.api;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class WaitForProjectionCommit
        implements Flow.Subscriber<WithMetadata<ProjectionCommit>>, AutoCloseable {

    record ProjectionWaiter(Optional<Long> version, Optional<Long> timestamp, CompletableFuture<Void> future) {
    }

    private static final Logger logger = LogManager.getLogger(WaitForProjectionCommit.class);
    private final String projectionId;
    private final BlockingQueue<ProjectionWaiter> queue = new ArrayBlockingQueue<>(1024);
    private Flow.Subscription subscription;
    private final AtomicLong currentVersion = new AtomicLong();
    private final AtomicLong currentTimestamp = new AtomicLong();

    public WaitForProjectionCommit(String projectionId) {
        this.projectionId = projectionId;
    }

    public CompletionStage<Void> waitForVersion(long version) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (currentVersion.get() >= version) {
            // Already done
            future.complete(null);
            return future;
        }

        try {
            queue.put(new ProjectionWaiter(Optional.of(version), Optional.empty(), future));
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletionStage<Void> waitForTimestamp(long timestamp) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (currentTimestamp.get() >= timestamp) {
            // Already done
            future.complete(null);
            return future;
        }

        try {
            queue.put(new ProjectionWaiter(Optional.empty(), Optional.of(timestamp), future));
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(64);
    }

    @Override
    public void onNext(WithMetadata<ProjectionCommit> item) {
        if (item.event().id().equals(projectionId)) {
            logger.debug("Received projection commit:"+item);

            currentVersion.set(item.event().revision());
            item.metadata().getLong("lastTimestamp").ifPresent(currentTimestamp::set);

            do {
                ProjectionWaiter waiter = queue.peek();
                if (waiter != null) {
                    if (waiter.version().map(v -> v <= currentVersion.get()).orElse(false)) {
                        waiter = queue.poll();
                        waiter.future().complete(null);
                    } else if (waiter.timestamp().map(v -> v <= currentTimestamp.get()).orElse(false)) {
                        waiter = queue.poll();
                        waiter.future().complete(null);
                    } else {
                        break;
                    }
                }
            } while (!queue.isEmpty());
        }

        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        onComplete();
    }

    @Override
    public void onComplete() {
        queue.forEach(waiter -> waiter.future().completeExceptionally(new IllegalStateException("Projection closed")));
    }

    @Override
    public void close() throws Exception {
        subscription.cancel();
    }
}
