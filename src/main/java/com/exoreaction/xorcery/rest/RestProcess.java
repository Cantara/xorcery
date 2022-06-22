package com.exoreaction.xorcery.rest;

import jakarta.ws.rs.ServerErrorException;
import org.apache.logging.log4j.LogManager;

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
            try {
                throw t;
            } catch (ServerErrorException e) {
                retry();
            } catch (Throwable e) {
                LogManager.getLogger(getClass()).error("Unhandled exception", e);
                result().toCompletableFuture().completeExceptionally(e);
            }
        } else {
            result().toCompletableFuture().complete(value);
        }
    }

    default void retry()
    {
        start();
    }
}
