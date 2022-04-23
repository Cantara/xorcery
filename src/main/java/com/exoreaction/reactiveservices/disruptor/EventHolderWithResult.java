package com.exoreaction.reactiveservices.disruptor;

import java.util.concurrent.CompletableFuture;

public class EventHolderWithResult<T,R>
    extends EventHolder<T>
{
    // Result
    public CompletableFuture<R> result;
}
