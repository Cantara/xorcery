package com.exoreaction.xorcery.service.reactivestreams.helper;


import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams.Subscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams.Subscription;
import com.lmax.disruptor.EventSink;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class MultiSubscriber<T> {

    private final Subscriber<T> delegate;
    private final EventSink<Event<T>> eventSink;

    private final List<SubscriberProxy<T>> proxies = new CopyOnWriteArrayList<>();

    private final AtomicLong oustandingRequestsTotal = new AtomicLong();

    public MultiSubscriber(Subscriber<T> delegate) {
        this.delegate = delegate;
        this.eventSink = delegate.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                oustandingRequestsTotal.addAndGet(n);

                allocateRequests();
            }

            @Override
            public void cancel() {
                for (SubscriberProxy<T> proxy : proxies) {
                    proxy.cancel();
                }
            }
        });
    }

    public void add(SubscriberProxy<T> subscriberProxy) {
        proxies.add(subscriberProxy);
    }

    void allocateRequests() {
        if (!proxies.isEmpty()) {
            long allowedRequestsPerProxy = oustandingRequestsTotal.get() / proxies.size();

            for (SubscriberProxy<T> proxy : proxies) {
                proxy.requests(allowedRequestsPerProxy);
            }
        }
    }

    public <T> void onError(SubscriberProxy subscriberProxy, Throwable throwable) {
        delegate.onError(throwable);
    }

    public <T> void onComplete(SubscriberProxy subscriberProxy) {
        proxies.remove(this);
        if (proxies.isEmpty())
            delegate.onComplete();
    }

    public Object invoke(Method method, Object[] args)
            throws Throwable {
        oustandingRequestsTotal.decrementAndGet();
        return method.invoke(eventSink, args);
    }

}
