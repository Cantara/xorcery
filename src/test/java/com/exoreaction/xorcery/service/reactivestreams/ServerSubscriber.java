package com.exoreaction.xorcery.service.reactivestreams;

import java.util.concurrent.Flow;

public abstract class ServerSubscriber<T>
        implements Flow.Subscriber<T> {

    protected Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}
