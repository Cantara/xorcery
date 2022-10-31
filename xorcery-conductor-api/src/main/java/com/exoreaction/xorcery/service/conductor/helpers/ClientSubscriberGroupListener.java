package com.exoreaction.xorcery.service.conductor.helpers;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;
import java.util.function.Function;

public class ClientSubscriberGroupListener extends AbstractGroupListener {
    private Class<? extends Flow.Subscriber<?>> subscriberType;
    private ReactiveStreams reactiveStreams;
    private Function<Configuration, Flow.Subscriber<?>> subscriberFactory;

    public ClientSubscriberGroupListener(ServiceIdentifier serviceIdentifier, Function<Configuration, Flow.Subscriber<?>> subscriberFactory, Class<? extends Flow.Subscriber<?>> subscriberType, String rel, ReactiveStreams reactiveStreams) {
        super(serviceIdentifier, rel);
        this.subscriberFactory = subscriberFactory;
        this.subscriberType = subscriberType;
        this.reactiveStreams = reactiveStreams;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
        reactiveStreams.subscribe(link.getHrefAsUri(), consumerConfiguration, subscriberFactory.apply(sourceConfiguration), subscriberType);
    }
}
