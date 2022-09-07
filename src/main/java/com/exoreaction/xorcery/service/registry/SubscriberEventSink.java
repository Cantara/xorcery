package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.Subscriber;
import com.lmax.disruptor.EventSink;

public record SubscriberEventSink<T>(Subscriber<T> subscriber, EventSink<WithMetadata<T>> sink) {
}
