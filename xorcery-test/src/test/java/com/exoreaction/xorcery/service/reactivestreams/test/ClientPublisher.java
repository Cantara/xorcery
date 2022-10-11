package com.exoreaction.xorcery.service.reactivestreams.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

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
