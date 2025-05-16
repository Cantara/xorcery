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
package dev.xorcery.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * This implements the "smart batching" concept, where items are being added to a queue, and a thread polls
 * the queue to handle them in batches. The size of the batch is determined by the amount of time it takes to handle a batch,
 * so under high load the batches will naturally be larger, and with lower load the batches will naturally be smaller.
 * <p>
 * We use an Executor to provide the separate thread, and once a batch is handled we resubmit the handler in order to avoid
 * starvation between many batchers using the same Executor.
 *
 * @param <T>
 */
public class SmartBatcher<T>
        implements AutoCloseable {
    private final Executor executor;

    private final BlockingQueue<T> queue;
    private final Lock updateLock = new ReentrantLock();
    private final Semaphore handlingLock = new Semaphore(1);
    private final AtomicBoolean closed = new AtomicBoolean();

    private final AtomicReference<RuntimeException> error = new AtomicReference<>();

    private final DrainQueue<T> drainQueue;

    public SmartBatcher(Consumer<Collection<T>> handler, BlockingQueue<T> queue, Executor executor) {
        this.executor = executor;
        this.queue = queue;
        this.drainQueue = new DrainQueue<T>(queue, updateLock, handlingLock, executor, handler, error);
    }

    public void submit(T item) throws InterruptedException {

        while (!queue.offer(item, 1, TimeUnit.SECONDS)) {
            if (closed.get())
                throw new InterruptedException();

            scheduleDrain();
        }

        scheduleDrain();
    }

    private void scheduleDrain() {
        try {
            updateLock.lock();

            if (error.get() instanceof IllegalStateException exception) {
                throw exception;
            }

            if (handlingLock.tryAcquire()) {
                executor.execute(drainQueue);
            }
        } finally {
            updateLock.unlock();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            while (error.get() == null && ((handlingLock.availablePermits() == 0 || !queue.isEmpty()))) {
//            System.out.println("Wait to shutdown");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }

    private static final class DrainQueue<T>
            implements Runnable {
        private final BlockingQueue<T> queue;
        private final Lock updateLock;
        private final Semaphore handlingLock;
        private final Executor executor;
        private final Consumer<Collection<T>> handler;
        private final AtomicReference<RuntimeException> error;
        private final List<T> batch = new ArrayList<>();

        DrainQueue(
                BlockingQueue<T> queue,
                Lock updateLock,
                Semaphore handlingLock,
                Executor executor,
                Consumer<Collection<T>> handler,
                AtomicReference<RuntimeException> error) {
            this.queue = queue;
            this.updateLock = updateLock;
            this.handlingLock = handlingLock;
            this.executor = executor;
            this.handler = handler;
            this.error = error;
        }

        @Override
        public void run() {
            try {
                queue.drainTo(batch, queue.size());
                handler.accept(batch);
                batch.clear();

                try {
                    updateLock.lock();
                    if (queue.isEmpty()) {
                        handlingLock.release();
                    } else {
                        executor.execute(this);
                    }
                } finally {
                    updateLock.unlock();
                }
            } catch (Throwable t) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR, "SmartBatcher handler failed", t);
                handlingLock.release();
                error.set(new IllegalStateException(t));
            }
        }
    }
}
