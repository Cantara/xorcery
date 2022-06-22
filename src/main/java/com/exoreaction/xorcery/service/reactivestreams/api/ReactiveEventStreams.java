package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.lmax.disruptor.EventSink;

public final class ReactiveEventStreams {
    private ReactiveEventStreams() {
    }

    @FunctionalInterface
    public interface Publisher<T> {
        public void subscribe(Subscriber<T> subscriber, Configuration parameters);
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
