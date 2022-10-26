package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public interface ReactiveStreamsClient {
    // Client
    CompletableFuture<Void> publish(URI subscriberWebsocketUri,
                                    Configuration subscriberConfiguration,
                                    Flow.Publisher<?> publisher,
                                    Class<? extends Flow.Publisher<?>> publisherType);

    CompletableFuture<Void> subscribe(URI publisherWebsocketUri,
                                    Configuration publisherConfiguration,
                                    Flow.Subscriber<?> subscriber,
                                    Class<? extends Flow.Subscriber<?>> subscriberType);
}
