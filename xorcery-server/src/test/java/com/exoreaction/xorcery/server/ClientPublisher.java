package com.exoreaction.xorcery.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ClientPublisher<T>
        implements Flow.Publisher<T> {

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
            }
        });

        CompletableFuture.runAsync(() ->
        {
            publish(subscriber);
        });
    }

    protected abstract void publish(Flow.Subscriber<? super T> subscriber);
}
