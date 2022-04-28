package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.lmax.disruptor.EventHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

public final class ReactiveEventStreams {
    private ReactiveEventStreams() {
    }

    @FunctionalInterface
    public interface Publisher<T> {
        public void subscribe(Subscriber<T> subscriber, Map<String, String> parameters);
    }

    public interface Subscriber<T> {
        public EventHandler<Event<T>> onSubscribe(Subscription subscription);
    }

    public interface Subscription {
        public void request(long n);

        public void cancel();
    }
}
