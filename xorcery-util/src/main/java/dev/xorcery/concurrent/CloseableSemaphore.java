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
