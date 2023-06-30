package com.exoreaction.xorcery.concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Will interrupt waiting threads on close. Useful for publishers that use semaphores to wait for sufficient requests.
 */
public class CloseableSemaphore
        extends Semaphore
        implements AutoCloseable {
    private volatile boolean isClosed;

    public CloseableSemaphore(int permits) {
        super(permits);
    }

    public CloseableSemaphore(int permits, boolean fair) {
        super(permits, fair);
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        if (isClosed)
            throw new InterruptedException("Closed");

        return super.tryAcquire(timeout, unit);
    }

    @Override
    public void close() {
        isClosed = true;
        for (Thread queuedThread : getQueuedThreads()) {
            queuedThread.interrupt();
        }
    }
}
