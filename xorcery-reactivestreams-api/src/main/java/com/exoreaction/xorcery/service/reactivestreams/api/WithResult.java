package com.exoreaction.xorcery.service.reactivestreams.api;

import java.util.concurrent.CompletableFuture;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class WithResult<T,R>
{
    private T event;
    private CompletableFuture<R> result;

    public WithResult() {
    }

    public WithResult(T event, CompletableFuture<R> result) {
        this.event = event;
        this.result = result;
    }

    public void set(T event, CompletableFuture<R> result)
    {
        this.event = event;
        this.result = result;
    }

    public void set(WithResult<T,R> other)
    {
        this.event = other.event;
        this.result = other.result;
    }

    public T event()
    {
        return event;
    }

    public CompletableFuture<R> result()
    {
        return result;
    }
}
