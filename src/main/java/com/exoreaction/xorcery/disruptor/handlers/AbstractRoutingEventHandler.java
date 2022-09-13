package com.exoreaction.xorcery.disruptor.handlers;

import com.lmax.disruptor.EventHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public abstract class AbstractRoutingEventHandler<T>
        implements EventHandler<T> {
    protected final List<SubscriptionTracker<T>> subscribers;

    public AbstractRoutingEventHandler() {
        this.subscribers = new CopyOnWriteArrayList<>();
    }

    public Flow.Subscription add(Flow.Subscriber<? super T> subscriber, Flow.Subscription subscription) {
        SubscriptionTracker<T> tracker = new SubscriptionTracker<>(subscriber, new AtomicLong());
        subscribers.add(tracker);
        return new Flow.Subscription() {
            @Override
            public void request(long n) {
                tracker.requests().addAndGet(n);
                subscription.request(n);
            }

            @Override
            public void cancel() {
                subscribers.remove(tracker);
                subscription.cancel();
            }
        };
    }

    public Flow.Subscription add(Flow.Subscriber<? super T> subscriber) {
        SubscriptionTracker<T> tracker = new SubscriptionTracker<>(subscriber, new AtomicLong());
        subscribers.add(tracker);
        return new Flow.Subscription() {
            @Override
            public void request(long n) {
                tracker.requests().addAndGet(n);
            }

            @Override
            public void cancel() {
                subscribers.remove(tracker);
                subscriber.onComplete();
            }
        };
    }

    public void remove(Flow.Subscriber<? super T> subscriber)
    {
        subscribers.removeIf(t -> t.subscriber() == subscriber);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onShutdown() {
        for (SubscriptionTracker<T> subscriber : subscribers) {
            subscriber.subscriber.onComplete();
        }
    }

    public record SubscriptionTracker<T>(Flow.Subscriber<? super T> subscriber, AtomicLong requests) {
    }
}
