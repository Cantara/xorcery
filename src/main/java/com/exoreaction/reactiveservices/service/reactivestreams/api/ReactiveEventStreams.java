package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.lmax.disruptor.EventSink;

import java.util.Map;

public final class ReactiveEventStreams {
    private ReactiveEventStreams() {
    }

    @FunctionalInterface
    public interface Publisher<T> {
        public void subscribe(Subscriber<T> subscriber, Map<String, String> parameters);
    }

    public interface Subscriber<T> {
        EventSink<Event<T>> onSubscribe(Subscription subscription);

        default void onError(Throwable throwable)
        {}

        default void onComplete()
        {}
    }

    public interface Subscription {
        public void request(long n);

        public void cancel();
    }
}
