package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public interface ReactiveStreamsClient {
    CompletableFuture<Void> publish(String toAuthority,
                                    String subscriberStreamName,
                                    Supplier<Configuration> subscriberServerConfiguration,

                                    Flow.Publisher<?> publisher,
                                    Class<? extends Flow.Publisher<?>> publisherType,
                                    ClientConfiguration publisherClientConfiguration);

    CompletableFuture<Void> subscribe(String toAuthority,
                                      String publisherStreamName,
                                      Supplier<Configuration> publisherServerConfiguration,

                                      Flow.Subscriber<?> subscriber,
                                      Class<? extends Flow.Subscriber<?>> subscriberType,
                                      ClientConfiguration subscriberClientConfiguration);
}
