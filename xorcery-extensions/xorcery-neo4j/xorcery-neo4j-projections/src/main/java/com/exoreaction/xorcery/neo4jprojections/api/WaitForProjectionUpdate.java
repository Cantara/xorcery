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

import com.exoreaction.xorcery.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.jsonapi.Meta;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.neo4jprojections.reactor.ProjectionStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ReactiveStreamsContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WaitForProjectionUpdate
        extends BaseSubscriber<Metadata>
        implements AutoCloseable {

    record ProjectionWaiter(Long position, Long timestamp, CompletableFuture<Metadata> future) {
    }

    private static final Logger logger = LogManager.getLogger(WaitForProjectionUpdate.class);

    private final String projectionId;

    private final BlockingQueue<ProjectionWaiter> queue = new ArrayBlockingQueue<>(1024);
    private final AtomicReference<Metadata> currentCommit = new AtomicReference<>();
    private final AtomicLong currentPosition = new AtomicLong();
    private final AtomicLong currentTimestamp = new AtomicLong();

    public WaitForProjectionUpdate(String projectionId) {
        this.projectionId = projectionId;
    }

    public CompletableFuture<Metadata> waitForPosition(long position) {
        CompletableFuture<Metadata> future = new CompletableFuture<>();
        if (currentPosition.get() >= position) {
            // Already done
            future.complete(currentCommit.get());
            return future;
        }

        try {
            queue.put(new ProjectionWaiter(position, null, future));
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public CompletableFuture<Metadata> waitForTimestamp(long timestamp) {
        CompletableFuture<Metadata> future = new CompletableFuture<>();
        if (currentTimestamp.get() >= timestamp) {
            // Already done
            future.complete(currentCommit.get());
            return future;
        }

        try {
            queue.put(new ProjectionWaiter(null, timestamp, future));
        } catch (InterruptedException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    protected void hookOnNext(Metadata item) {
        if (item.getString(ProjectionStreamContext.projectionId).map(id -> id.equals(projectionId)).orElse(false)) {
            if (logger.isDebugEnabled())
                logger.debug("Received projection update: " + item.getString(ProjectionStreamContext.projectionId).orElse("") + ":" + item.getLong(ReactiveStreamsContext.streamPosition).orElse(0L));

            currentCommit.set(item);
            item.getLong(ReactiveStreamsContext.streamPosition).ifPresent(currentPosition::set);
            item.getLong(DomainEventMetadata.timestamp).ifPresent(currentTimestamp::set);

            do {
                ProjectionWaiter waiter = queue.peek();
                if (waiter != null) {
                    if (waiter.position() != null && waiter.position() <= currentPosition.get()) {
                        waiter = queue.poll();
                        waiter.future().complete(item);
                    } else if (waiter.timestamp() != null && waiter.timestamp() <= currentTimestamp.get()) {
                        waiter = queue.poll();
                        waiter.future().complete(item);
                    } else {
                        break;
                    }
                }
            } while (!queue.isEmpty());
        }
    }

    @Override
    protected void hookOnComplete() {
        queue.forEach(waiter -> waiter.future().completeExceptionally(new IllegalStateException("Projection closed")));
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        hookOnComplete();
    }

    @Override
    protected void hookOnCancel() {
        hookOnComplete();
    }

    @Override
    public void close() throws Exception {
        cancel();
    }
}
