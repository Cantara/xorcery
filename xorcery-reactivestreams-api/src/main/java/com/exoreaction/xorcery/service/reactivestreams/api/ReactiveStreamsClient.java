package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ReactiveStreamsClient {
    CompletableFuture<Void> publish(String toAuthority,
                                    String subscriberStreamName,
                                    Supplier<Configuration> subscriberConfiguration,

                                    Flow.Publisher<?> publisher,
                                    Class<? extends Flow.Publisher<?>> publisherType,
                                    Configuration publisherConfiguration);

    CompletableFuture<Void> subscribe(String toAuthority,
                                      String publisherStreamName,
                                      Supplier<Configuration> publisherConfiguration,

                                      Flow.Subscriber<?> subscriber,
                                      Class<? extends Flow.Subscriber<?>> subscriberType,
                                      Configuration subscriberConfiguration);
}
