package com.exoreaction.xorcery.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final Consumer<Collection<T>> handler;
    private final Executor executor;

    private final BlockingQueue<T> queue;

    private final Lock updateLock = new ReentrantLock();
    private final Semaphore handlingLock = new Semaphore(1);
    private final AtomicBoolean closed = new AtomicBoolean();

    private final List<T> batch = new ArrayList<>();

    public SmartBatcher(Consumer<Collection<T>> handler, BlockingQueue<T> queue, Executor executor) {
        this.handler = handler;
        this.executor = executor;
        this.queue = queue;
    }

    public void submit(T item) throws InterruptedException {

        while (!queue.offer(item, 1, TimeUnit.SECONDS))
        {
            if (closed.get())
                throw new InterruptedException();
        }

        try {
            updateLock.lock();
            if (handlingLock.tryAcquire()) {
                executor.execute(this::drainQueue);
            }
        } finally {
            updateLock.unlock();
        }
    }

    private void drainQueue() {
        try {
            queue.drainTo(batch, queue.size());
            handler.accept(batch);
            batch.clear();

            try {
                updateLock.lock();
                if (queue.isEmpty()) {
                    handlingLock.release();
                } else {
                    executor.execute(this::drainQueue);
                }
            } finally {
                updateLock.unlock();
            }
        } catch (Throwable t) {
            System.getLogger(getClass().getName()).log(System.Logger.Level.ERROR, "SmartBatcher handler failed", t);
            handlingLock.release();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true))
        {
            int tries = 0;
            while ((handlingLock.availablePermits() == 0 || !queue.isEmpty()) && tries++ < 100) {
//            System.out.println("Wait to shutdown");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }
}
