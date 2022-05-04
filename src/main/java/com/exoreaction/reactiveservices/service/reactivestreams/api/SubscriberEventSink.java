package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.lmax.disruptor.EventSink;

public record SubscriberEventSink<T>(ReactiveEventStreams.Subscriber<T> subscriber, EventSink<Event<T>> sink) {
}
