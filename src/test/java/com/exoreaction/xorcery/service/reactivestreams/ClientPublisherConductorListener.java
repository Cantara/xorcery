package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;

public class ClientPublisherConductorListener<T> extends AbstractConductorListener {
    private ReactiveEventStreams.Publisher<T> publisher;
    private ReactiveStreams reactiveStreams;
    private final ServiceIdentifier serviceIdentifier;

    public ClientPublisherConductorListener(ServiceIdentifier serviceIdentifier, ReactiveEventStreams.Publisher<T> publisher, String rel, ReactiveStreams reactiveStreams) {
        super(serviceIdentifier, rel);
        this.publisher = publisher;
        this.reactiveStreams = reactiveStreams;
        this.serviceIdentifier = serviceIdentifier;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
        reactiveStreams.publish(serviceIdentifier, link, publisher, sourceConfiguration, consumerConfiguration);
    }
}
