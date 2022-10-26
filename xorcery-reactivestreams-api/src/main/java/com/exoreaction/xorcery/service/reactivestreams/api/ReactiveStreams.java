package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;

public interface ReactiveStreams
    extends ReactiveStreamsClient
{
    // Server
    CompletableFuture<Void> publisher(String publisherWebsocketPath,
                                      Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory,
                                      Class<? extends Flow.Publisher<?>> publisherType);

    CompletableFuture<Void> subscriber(String subscriberWebsocketPath,
                                       Function<Configuration, Flow.Subscriber<?>> subscriberFactory,
                                       Class<? extends Flow.Subscriber<?>> subscriberType);
}
