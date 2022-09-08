package com.exoreaction.xorcery.service.reactivestreams;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ClientPublisher<T>
        implements Flow.Publisher<T> {

    private final CompletableFuture<Void> done = new CompletableFuture<>();

    public CompletableFuture<Void> getDone() {
        return done;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Semaphore semaphore = new Semaphore(0);
        AtomicLong initialRequest = new AtomicLong(-1);

        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                semaphore.release((int) n);
                if (initialRequest.get() == -1) {
                    initialRequest.set(n);
                }
            }

            @Override
            public void cancel() {

            }
        });

        CompletableFuture.runAsync(() ->
        {
            publish(subscriber);
        }).whenComplete((r, t) -> done.complete(null));
    }

    protected abstract void publish(Flow.Subscriber<? super T> subscriber);
}
