package com.exoreaction.reactiveservices.jetty.client;

import com.exoreaction.util.builders.With;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.concurrent.CompletableFuture;

public record WriteCallbackCompletableFuture(CompletableFuture<Void> future)
    implements WriteCallback, With<WriteCallbackCompletableFuture>
{
    public WriteCallbackCompletableFuture() {
        this(new CompletableFuture<>());
    }

    @Override
    public void writeFailed(Throwable t) {
        future.completeExceptionally(t);
    }

    @Override
    public void writeSuccess() {
        future.complete(null);
    }
}
