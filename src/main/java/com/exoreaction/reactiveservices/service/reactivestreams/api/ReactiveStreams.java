package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.spi.Contract;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Contract
public interface ReactiveStreams {
    <T> void publish(ServiceIdentifier selfServiceIdentifier, Link websocketLink, Publisher<T> publisher);

    <T> CompletionStage<Void> subscribe(ServiceIdentifier selfServiceIdentifier, Link websocketLink,
                                        ReactiveEventStreams.Subscriber<T> subscriber, Configuration configuration);

    default <T> CompletionStage<Void> subscribe(ServiceIdentifier selfServiceIdentifier, Link websocketLink,
                               ReactiveEventStreams.Subscriber<T> subscriber) {
        return subscribe(selfServiceIdentifier, websocketLink, subscriber, new Configuration(JsonNodeFactory.instance.objectNode()));
    }
}
