package com.exoreaction.reactiveservices.service.reactivestreams.helper;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.lmax.disruptor.EventSink;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SubscriberProxy<T>
        implements ReactiveEventStreams.Subscriber<T>, InvocationHandler {
    private final MultiSubscriber<T> multiSubscriber;
    private final AtomicLong oustandingRequests = new AtomicLong();
    private ReactiveEventStreams.Subscription subscription;

    public SubscriberProxy(MultiSubscriber<T> multiSubscriber) {
        this.multiSubscriber = multiSubscriber;
        multiSubscriber.add(this);
    }

    public void requests(long allowedOutstandingRequests) {
        long toBeRequested = allowedOutstandingRequests - oustandingRequests.get();
        if (toBeRequested > 0) {
            subscription.request(toBeRequested);
            oustandingRequests.addAndGet(toBeRequested);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        oustandingRequests.decrementAndGet();
        return multiSubscriber.invoke(method, args);
    }

    @Override
    public EventSink<Event<T>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        this.subscription = subscription;
        try {
            return (EventSink<Event<T>>) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{EventSink.class}, this);
        } finally {
            multiSubscriber.allocateRequests();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        multiSubscriber.onError(this, throwable);
    }

    @Override
    public void onComplete() {
        multiSubscriber.onComplete(this);
    }

    public void cancel() {
        subscription.cancel();
    }
}
