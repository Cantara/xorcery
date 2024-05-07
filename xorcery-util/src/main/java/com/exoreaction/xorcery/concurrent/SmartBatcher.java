package com.exoreaction.xorcery.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class SmartBatcher<T>
        implements AutoCloseable {
    private final Consumer<Collection<T>> handler;
    private final Executor executor;

    private final BlockingQueue<T> queue;
    private final Lock updateLock = new ReentrantLock();
    private final Semaphore handlingLock = new Semaphore(1);
    private final List<T> batch = new ArrayList<>();

    public SmartBatcher(Consumer<Collection<T>> handler, BlockingQueue<T> queue, Executor executor) {
        this.handler = handler;
        this.executor = executor;
        this.queue = queue;
    }

    public void submit(T item) throws InterruptedException {
        queue.put(item);
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
            queue.drainTo(batch);
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
            handlingLock.release();
        }
    }

    @Override
    public void close() {
        while (handlingLock.availablePermits() == 0 || !queue.isEmpty()) {
//            System.out.println("Wait to shutdown");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }
}
