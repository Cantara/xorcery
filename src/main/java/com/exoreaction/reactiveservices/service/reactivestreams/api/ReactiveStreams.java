package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.model.ServiceAttributes;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import org.glassfish.jersey.spi.Contract;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Contract
public interface ReactiveStreams {
    <T> void publish(ServiceIdentifier selfServiceIdentifier, Link websocketLink, Publisher<T> publisher);

    <T> void subscribe(ServiceIdentifier selfServiceIdentifier, Link websocketLink,
                       ReactiveEventStreams.Subscriber<T> subscriber, Map<String, String> parameters);

    default <T> void subscribe(ServiceIdentifier selfServiceIdentifier, Link websocketLink,
                       ReactiveEventStreams.Subscriber<T> subscriber, Optional<ServiceAttributes> attributes)
    {
        subscribe(selfServiceIdentifier, websocketLink, subscriber,
                attributes.map(ServiceAttributes::toMap).orElse(Collections.emptyMap()));
    }

    default <T> void subscribe(ServiceIdentifier selfServiceIdentifier, Link websocketLink,
                               ReactiveEventStreams.Subscriber<T> subscriber) {
        subscribe(selfServiceIdentifier, websocketLink, subscriber, Collections.emptyMap());
    }
}
