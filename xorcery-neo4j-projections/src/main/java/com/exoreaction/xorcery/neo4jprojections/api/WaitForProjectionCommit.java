/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.neo4jprojections.api;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class WaitForProjectionCommit
        implements Subscriber<WithMetadata<ProjectionCommit>>, AutoCloseable {

    record ProjectionWaiter(Optional<Long> version, Optional<Long> timestamp,
                            CompletableFuture<WithMetadata<ProjectionCommit>> future) {
    }

    private static final Logger logger = LogManager.getLogger(WaitForProjectionCommit.class);
    private final String projectionId;
    private final BlockingQueue<ProjectionWaiter> queue = new ArrayBlockingQueue<>(1024);
    private Subscription subscription;
    private final AtomicReference<WithMetadata<ProjectionCommit>> currentCommit = new AtomicReference<>();
    private final AtomicLong currentVersion = new AtomicLong();
    private final AtomicLong currentTimestamp = new AtomicLong();

    public WaitForProjectionCommit(String projectionId) {
        this.projectionId = projectionId;
    }

    public CompletableFuture<WithMetadata<ProjectionCommit>> waitForVersion(long version) {
        CompletableFuture<WithMetadata<ProjectionCommit>> future = new CompletableFuture<>();
        if (currentVersion.get() >= version) {
            // Already done
            future.complete(currentCommit.get());
            return future;
        }

        try {
            queue.put(new ProjectionWaiter(Optional.of(version), Optional.empty(), future));
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<WithMetadata<ProjectionCommit>> waitForTimestamp(long timestamp) {
        CompletableFuture<WithMetadata<ProjectionCommit>> future = new CompletableFuture<>();
        if (currentTimestamp.get() >= timestamp) {
            // Already done
            future.complete(currentCommit.get());
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
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(64);
    }

    @Override
    public void onNext(WithMetadata<ProjectionCommit> item) {
        if (item.event().id().equals(projectionId)) {
            logger.debug("Received projection commit: " + item.event().id()+":"+item.event().revision()+"("+item.metadata().json().toPrettyString()+")");

            currentCommit.set(item);
            currentVersion.set(item.event().revision());
            item.metadata().getLong("lastTimestamp").ifPresent(currentTimestamp::set);

            do {
                ProjectionWaiter waiter = queue.peek();
                if (waiter != null) {
                    if (waiter.version().map(v -> v <= currentVersion.get()).orElse(false)) {
                        waiter = queue.poll();
                        waiter.future().complete(item);
                    } else if (waiter.timestamp().map(v -> v <= currentTimestamp.get()).orElse(false)) {
                        waiter = queue.poll();
                        waiter.future().complete(item);
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
