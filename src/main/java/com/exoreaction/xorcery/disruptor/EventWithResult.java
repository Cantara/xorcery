package com.exoreaction.xorcery.disruptor;

import java.util.concurrent.CompletableFuture;

public record EventWithResult<T,R>(T event, CompletableFuture<R> result)
{
}
