package com.exoreaction.xorcery.service.reactivestreams.helper;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;

import java.util.concurrent.Flow;
import java.util.function.Function;

public class ClientPublisherConductorListener extends AbstractConductorListener {
    private Class<? extends Flow.Publisher<?>> publisherType;
    private ReactiveStreams reactiveStreams;
    private Function<Configuration, Flow.Publisher<?>> publisherFactory;

    public ClientPublisherConductorListener(ServiceIdentifier serviceIdentifier, Function<Configuration, Flow.Publisher<?>> publisherFactory, Class<? extends Flow.Publisher<?>> publisherType, String rel, ReactiveStreams reactiveStreams) {
        super(serviceIdentifier, rel);
        this.publisherFactory = publisherFactory;
        this.publisherType = publisherType;
        this.reactiveStreams = reactiveStreams;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
        reactiveStreams.publish(link.getHrefAsUri(), consumerConfiguration, publisherFactory.apply(sourceConfiguration), publisherType);
    }
}
