package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import org.apache.logging.log4j.Marker;
import org.glassfish.jersey.spi.Contract;

import java.util.Collections;
import java.util.Map;

@Contract
public interface ReactiveStreams {
    <T> void publish(ServiceIdentifier selfServiceIdentifier, Link websocketLink, Publisher<T> publisher);

    <T> void subscribe(ServiceIdentifier selfServiceIdentifier, Link websocketLink,
                       ReactiveEventStreams.Subscriber<T> subscriber, Map<String, String> parameters);

    default <T> void subscribe(ServiceIdentifier selfServiceIdentifier, Link websocketLink,
                               ReactiveEventStreams.Subscriber<T> subscriber) {
        subscribe(selfServiceIdentifier, websocketLink, subscriber, Collections.emptyMap());
    }
}
