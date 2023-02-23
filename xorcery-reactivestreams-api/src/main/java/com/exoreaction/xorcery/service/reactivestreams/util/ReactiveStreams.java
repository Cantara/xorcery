package com.exoreaction.xorcery.service.reactivestreams.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface ReactiveStreams {
    static <T> Function<Throwable, CompletionStage<T>> cancelStream(CompletableFuture<Void> streamFuture) {
        return t ->
        {
            streamFuture.cancel(true);
            return CompletableFuture.failedStage(t);
        };
    }
}
