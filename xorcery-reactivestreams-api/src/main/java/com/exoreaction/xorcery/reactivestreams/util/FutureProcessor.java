package com.exoreaction.xorcery.reactivestreams.util;

import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Flow.Processor that tracks the state of the stream with a CompletableFuture.
 * <p>
 * Can either be triggered by the future, or complete the future itself.
 *
 * @param <T>
 */
public class FutureProcessor<T>
        implements Flow.Processor<T, T>, Flow.Subscription, AutoCloseable {
    private final CompletableFuture<Void> result;
    private Flow.Subscriber<? super T> subscriber;
    private Flow.Subscription subscription;

    public FutureProcessor(CompletableFuture<Void> result) {
        this.result = result;
        result.whenComplete(this::whenComplete);
    }

    public CompletableFuture<Void> getResult() {
        return result;
    }

    public synchronized Flow.Subscriber<? super T> getSubscriber() {
        return subscriber;
    }

    public synchronized Flow.Subscription getSubscription() {
        return subscription;
    }

    private synchronized void whenComplete(Void result, Throwable throwable) {
        cancel();
    }

    // Publisher
    @Override
    public synchronized void subscribe(Flow.Subscriber<? super T> subscriber) {
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
    public synchronized void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        if (result.isDone()) {
            subscription.cancel();
        } else if (subscriber != null)
            subscriber.onSubscribe(this);
    }

    @Override
    public synchronized void onNext(T item) {
        subscriber.onNext(item);
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        if (subscriber != null)
        {
            subscription = null;
            subscriber.onError(throwable);
        }
        if (!result.isDone())
            result.completeExceptionally(throwable);
    }

    @Override
    public synchronized void onComplete() {
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
        Flow.Subscription sub = subscription;
        if (sub != null) {
            sub.request(n);
        }
    }

    @Override
    public void cancel() {
        Flow.Subscription sub = subscription;
        if (sub != null) {
            subscription = null;
            sub.cancel();
        }
        if (!result.isDone())
            result.cancel(true);
    }

    // Closeable
    @Override
    public synchronized void close() {
        if (subscriber != null)
        {
            Flow.Subscriber<? super T> sub = subscriber;
            subscriber = null;
            cancel();
            ServerShutdownStreamException exception = new ServerShutdownStreamException("shutdown");
            sub.onError(exception);
        }
//        cancel();
    }
}
