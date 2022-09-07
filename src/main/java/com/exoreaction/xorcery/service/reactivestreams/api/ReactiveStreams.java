package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.Configuration;
import org.glassfish.jersey.spi.Contract;

import java.net.URI;
import java.util.concurrent.CompletionStage;

@Contract
public interface ReactiveStreams {
    // Server
    <T> CompletionStage<Void> publisher(String publisherWebsocketPath,
                                        Publisher<T> publisher);

    <T> CompletionStage<Void> subscriber(String subscriberWebsocketPath,
                                         Subscriber<T> subscriber);

    // Client
    <T> CompletionStage<Void> publish(URI subscriberWebsocketUri,
                                      Publisher<T> publisher,
                                      Configuration publisherConfiguration,
                                      Configuration subscriberConfiguration);

    <T> CompletionStage<Void> subscribe(URI publisherWebsocketUri,
                                        Subscriber<T> subscriber,
                                        Configuration publisherConfiguration,
                                        Configuration subscriberConfiguration);
}
