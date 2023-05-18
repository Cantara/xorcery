package com.exoreaction.xorcery.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public final class CompletableFutures {

    public static <T> BiConsumer<? super T, ? super Throwable> transfer(CompletableFuture<? super T> result) {
        return (value, throwable) ->
        {
            if (throwable != null) result.completeExceptionally(throwable);
            else result.complete(value);
        };
    }
}