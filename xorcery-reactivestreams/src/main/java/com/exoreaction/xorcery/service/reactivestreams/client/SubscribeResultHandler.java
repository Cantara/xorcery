package com.exoreaction.xorcery.service.reactivestreams.client;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

class SubscribeResultHandler implements Flow.Subscriber<Object> {
    private final Flow.Subscriber<Object> subscriber;
    private CompletableFuture<Void> result;
    private MessageWriter<Object> writer;
    private MessageReader<Object> reader;
    private Flow.Subscription subscription;

    public SubscribeResultHandler(Flow.Subscriber<Object> subscriber, CompletableFuture<Void> result) {
        this.subscriber = subscriber;
        this.result = result;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(Object item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
        result.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
        result.complete(null);
    }
}
