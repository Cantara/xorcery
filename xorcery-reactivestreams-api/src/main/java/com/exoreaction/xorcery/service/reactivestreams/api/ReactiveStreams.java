package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.Configuration;
import org.glassfish.jersey.spi.Contract;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;

@Contract
public interface ReactiveStreams {
    // Server
    CompletableFuture<Void> publisher(String publisherWebsocketPath,
                                      Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory,
                                      Class<? extends Flow.Publisher<?>> publisherType);

    CompletableFuture<Void> subscriber(String subscriberWebsocketPath,
                                       Function<Configuration, Flow.Subscriber<?>> subscriberFactory,
                                       Class<? extends Flow.Subscriber<?>> subscriberType);

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
