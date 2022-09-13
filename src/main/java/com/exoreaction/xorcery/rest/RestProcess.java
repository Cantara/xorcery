package com.exoreaction.xorcery.rest;

import jakarta.ws.rs.ClientErrorException;
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
                throw unwrap(t);
            } catch (ServerErrorException e) {
                retry();
            } catch (ClientErrorException e) {
                result().toCompletableFuture().completeExceptionally(e);
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

    default Throwable unwrap(Throwable t)
    {
        while (t.getCause() != null)
        {
            t = t.getCause();
        }
        return t;
    }
}
