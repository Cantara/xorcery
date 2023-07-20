/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.disruptor.handlers;

import com.lmax.disruptor.EventHandler;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    public Subscription add(Subscriber<? super T> subscriber, Subscription subscription) {
        SubscriptionTracker<T> tracker = new SubscriptionTracker<>(subscriber, new AtomicLong());
        subscribers.add(tracker);
        return new Subscription() {
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

    public Subscription add(Subscriber<? super T> subscriber) {
        SubscriptionTracker<T> tracker = new SubscriptionTracker<>(subscriber, new AtomicLong());
        subscribers.add(tracker);
        return new Subscription() {
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

    public void remove(Subscriber<? super T> subscriber)
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

    public record SubscriptionTracker<T>(Subscriber<? super T> subscriber, AtomicLong requests) {
    }
}
