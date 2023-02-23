package com.exoreaction.xorcery.service.reactivestreams.client;

import com.exoreaction.xorcery.builders.With;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class WriteCallbackCompletableFuture
        implements WriteCallback, With<WriteCallbackCompletableFuture> {
    private final CompletableFuture<Void> future;

    public WriteCallbackCompletableFuture(CompletableFuture<Void> future) {
        this.future = future;
    }

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

    public CompletableFuture<Void> future() {
        return future;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WriteCallbackCompletableFuture) obj;
        return Objects.equals(this.future, that.future);
    }

    @Override
    public int hashCode() {
        return Objects.hash(future);
    }

    @Override
    public String toString() {
        return "WriteCallbackCompletableFuture[" +
               "future=" + future + ']';
    }

}
