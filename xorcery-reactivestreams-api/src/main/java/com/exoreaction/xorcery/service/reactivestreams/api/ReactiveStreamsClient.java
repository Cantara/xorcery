package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.glassfish.jersey.spi.Contract;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

@Contract
public interface ReactiveStreamsClient {
    // Client
    CompletionStage<Void> publish(URI subscriberWebsocketUri,
                                  Configuration subscriberConfiguration,
                                  Flow.Publisher<?> publisher,
                                  Class<? extends Flow.Publisher<?>> publisherType);

    CompletionStage<Void> subscribe(URI publisherWebsocketUri,
                                    Configuration publisherConfiguration,
                                    Flow.Subscriber<?> subscriber,
                                    Class<? extends Flow.Subscriber<?>> subscriberType);
}
