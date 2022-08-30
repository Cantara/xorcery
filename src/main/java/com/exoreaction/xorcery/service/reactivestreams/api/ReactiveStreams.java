package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.CompletionStage;

@Contract
public interface ReactiveStreams {
    <T> CompletionStage<Void> publisher(ServiceIdentifier selfServiceIdentifier,
                                        Link publisherWebsocketLink,
                                        Publisher<T> publisher);

    <T> CompletionStage<Void> subscribe(ServiceIdentifier selfServiceIdentifier,
                                        Link publisherWebsocketLink,
                                        ReactiveEventStreams.Subscriber<T> subscriber,
                                        Configuration publisherConfiguration,
                                        Configuration subscriberConfiguration);

    // Inverted (publishers connects to subscribers)
    <T> CompletionStage<Void> subscriber(ServiceIdentifier selfServiceIdentifier,
                                         Link subscriberWebsocketLink,
                                        ReactiveEventStreams.Subscriber<T> subscriber);

    <T> CompletionStage<Void> publish(ServiceIdentifier selfServiceIdentifier,
                                      Link subscriberWebsocketLink,
                                      Publisher<T> publisher,
                                      Configuration publisherConfiguration,
                                      Configuration subscriberConfiguration);
}
