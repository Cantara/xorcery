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
package com.exoreaction.xorcery.reactivestreams.util;

import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

/**
 * Processor that tracks the state of the stream with a CompletableFuture.
 * <p>
 * Can either be triggered by the future, or complete the future itself.
 *
 * @param <T>
 */
public class FutureProcessor<T>
        implements Processor<T, T>, Subscription, AutoCloseable {
    private final CompletableFuture<Void> result;
    private Subscriber<? super T> subscriber;
    private Subscription subscription;

    public FutureProcessor(CompletableFuture<Void> result) {
        this.result = result;
        result.whenComplete(this::whenComplete);
    }

    public CompletableFuture<Void> getResult() {
        return result;
    }

    public synchronized Subscriber<? super T> getSubscriber() {
        return subscriber;
    }

    public synchronized Subscription getSubscription() {
        return subscription;
    }

    private synchronized void whenComplete(Void result, Throwable throwable) {
        cancel();
    }

    // Publisher
    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        if (subscription != null)
            subscriber.onSubscribe(this);
        else if (result.isDone()) {
            try {
                result.join();
                subscriber.onComplete();
            } catch (Exception e) {
                subscriber.onError(e.getCause());
            }
        }
    }

    // Subscriber
    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        if (result.isDone()) {
            subscription.cancel();
        } else if (subscriber != null)
            subscriber.onSubscribe(this);
    }

    @Override
    public void onNext(T item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscriber != null)
        {
            subscription = null;
            subscriber.onError(throwable);
        }
        if (!result.isDone())
            result.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        if (subscriber != null)
        {
            subscription = null;
            subscriber.onComplete();
        }
        if (!result.isDone())
            result.complete(null);
    }

    // Subscription
    @Override
    public void request(long n) {
        Subscription sub = subscription;
        if (sub != null) {
            sub.request(n);
        }
    }

    @Override
    public void cancel() {
        Subscription sub = subscription;
        if (sub != null) {
            subscription = null;
            sub.cancel();
        }
        if (!result.isDone())
            result.cancel(true);
    }

    // Closeable
    @Override
    public void close() {
        if (subscriber != null && subscription != null)
        {
            Subscriber<? super T> sub = subscriber;
            subscriber = null;
            cancel();
            ServerShutdownStreamException exception = new ServerShutdownStreamException("shutdown");
            sub.onError(exception);
        }
//        cancel();
    }
}
