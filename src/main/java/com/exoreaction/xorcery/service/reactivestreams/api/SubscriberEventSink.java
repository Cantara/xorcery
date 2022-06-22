package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.disruptor.Event;
import com.lmax.disruptor.EventSink;

public record SubscriberEventSink<T>(ReactiveEventStreams.Subscriber<T> subscriber, EventSink<Event<T>> sink) {
}
