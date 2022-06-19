package com.exoreaction.reactiveservices.service.neo4jprojections;

import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class WaitForProjections implements ProjectionListener {

    record ProjectionWaiter(Metadata metadata, CompletableFuture<Metadata> future) {
    }

    BlockingQueue<ProjectionWaiter> queue = new ArrayBlockingQueue<>(1024);
    Map<String, Long> currentRevisions = new ConcurrentHashMap<>();

    public CompletionStage<Metadata> waitFor(Metadata metadata) {
        long revision = metadata.getLong("revision").orElse(0L);
        String streamId = metadata.getString("streamId").orElse("$all");

        CompletableFuture<Metadata> future = new CompletableFuture<>();
        if (Optional.ofNullable(currentRevisions.get(streamId)).orElse(0L) > revision) {
            // Already done
            future.complete(metadata);
            return future;
        }

        try {
            queue.put(new ProjectionWaiter(metadata, future));
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public void onCommit(String streamId, long revision) {
        currentRevisions.put(streamId, revision);

        do {
            ProjectionWaiter waiter = queue.peek();
            if (waiter != null) {
                if (waiter.metadata().getLong("revision").orElse(0L) <= revision) {
                    waiter = queue.poll();
                    waiter.future().complete(waiter.metadata);
                } else {
                    break;
                }
            }
        } while (!queue.isEmpty());
    }
}
