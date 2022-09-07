package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.Configuration;
import org.glassfish.jersey.spi.Contract;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

@Contract
public interface ReactiveStreams2 {
    // Server
    CompletionStage<Void> publisher(String publisherWebsocketPath,
                                    Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory);

    CompletionStage<Void> subscriber(String subscriberWebsocketPath,
                                     Function<Configuration, Flow.Subscriber<?>> subscriberFactory);

    // Client
    CompletionStage<Void> publish(URI subscriberWebsocketUri,
                                  Configuration subscriberConfiguration,
                                  Flow.Publisher<?> publisher);

    CompletionStage<Void> subscribe(URI publisherWebsocketUri,
                                    Configuration publisherConfiguration,
                                    Flow.Subscriber<?> subscriber);
}
