package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.Configuration;

public interface Publisher<T> {
    void subscribe(Subscriber<T> subscriber, Configuration publisherConfiguration, Configuration subscriberConfiguration);
}
