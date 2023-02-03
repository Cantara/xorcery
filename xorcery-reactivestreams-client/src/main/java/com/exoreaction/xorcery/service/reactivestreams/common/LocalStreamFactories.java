package com.exoreaction.xorcery.service.reactivestreams.common;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * This is used by the reactive stream client to get access to local stream factories, i.e. when authority is set to null.
 */
public interface LocalStreamFactories {

    WrappedSubscriberFactory getSubscriberFactory(String streamName);

    WrappedPublisherFactory getPublisherFactory(String streamName);

    record WrappedPublisherFactory(Function<Configuration, ? extends Flow.Publisher<Object>> factory,
                                   Class<? extends Flow.Publisher<?>> publisherType) {
    }

    record WrappedSubscriberFactory(Function<Configuration, Flow.Subscriber<Object>> factory,
                                    Class<? extends Flow.Subscriber<?>> subscriberType) {
    }
}
