package com.exoreaction.xorcery.rest;

import jakarta.ws.rs.ServerErrorException;

import java.util.concurrent.CompletionStage;

public interface RestProcess<T> {
    void start();

    default void stop() {
        result().toCompletableFuture().cancel(true);
    }

    CompletionStage<T> result();

    default void complete(T value, Throwable t) {

        if (result().toCompletableFuture().isCancelled())
            return;

        if (t != null) {
            t = unwrap(t);
            if (isRetryable(t)) {
                retry();
            } else {
                result().toCompletableFuture().completeExceptionally(t);
            }
        } else {
            result().toCompletableFuture().complete(value);
        }
    }

    default void retry() {
        start();
    }

    default boolean isRetryable(Throwable t) {
        return t instanceof ServerErrorException;
    }

    default Throwable unwrap(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }
}
