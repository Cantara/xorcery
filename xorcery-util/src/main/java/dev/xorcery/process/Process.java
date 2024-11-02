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
package dev.xorcery.process;

import dev.xorcery.lang.Exceptions;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Long-running process that normally involves I/O to other systems.
 * Can perform optional retries with backoff if possible, and can be cancelled through the provided future.
 * If the process generates a result this can be returned using the provided future as well.
 *
 * @param <T> type of the result, may be Void
 */
public interface Process<T> {

    Map<Process<?>, AtomicLong> failures = Collections.synchronizedMap(new WeakHashMap<>());

    CompletableFuture<T> result();

    void start();

    default void stop() {
        done();
        result().cancel(true);
    }

    default void done() {
        failures.remove(this);
    }

    default void retry() {

        if (result().isCancelled())
        {
            done();
            return;
        }

        AtomicLong nrOfFailures = failures.computeIfAbsent(this, k -> new AtomicLong(0L));
        long delay = getFailureDelay(nrOfFailures.getAndIncrement());
        if (delay == 0) {
            debug("Retrying");
        } else {
            debug(String.format("Retrying in %ds", delay));
        }

        CompletableFuture.delayedExecutor(delay, TimeUnit.SECONDS).execute(() ->
        {
            if (result().isCancelled())
            {
                done();
                return;
            }

            start();
        });
    }

    default long getFailureDelay(long failureCount) {
        return Math.min(failureCount * 10L, 60L);
    }

    default boolean isRetryable(Throwable t) {
        return false;
    }

    default void complete(T value, Throwable t) {

        if (result().isCancelled()) {
            done();
            return;
        }

        if (t != null) {
            t = Exceptions.unwrap(t);
            error(t);
            if (isRetryable(t)) {
                retry();
            } else {
                done();
                result().completeExceptionally(t);
            }
        } else {
            done();
            result().complete(value);
        }
    }

    // Logging
    default void error(Throwable throwable) {
    }

    default void debug(String message) {
    }
}
