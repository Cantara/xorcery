package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.Configuration;

public interface Subscriber<T> {
    SubscriptionEventSink<T> onSubscribe(Subscription subscription, Configuration subscriberConfiguration);
}
