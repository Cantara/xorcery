package com.exoreaction.xorcery.service.opensearch.requestlog;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams2;

public class RequestLogConductorListener extends AbstractConductorListener {
    private OpenSearchClient client;
    private ReactiveStreams2 reactiveStreams;
    private final ServiceIdentifier serviceIdentifier;
    private final String rel;

    public RequestLogConductorListener(OpenSearchClient client, ReactiveStreams2 reactiveStreams,
                                       ServiceIdentifier serviceIdentifier, String rel) {
        super(serviceIdentifier, rel);
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.serviceIdentifier = serviceIdentifier;
        this.rel = rel;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
        reactiveStreams.subscribe(link.getHrefAsUri(), sourceConfiguration, new RequestLogEventsSubscriber(client, consumerConfiguration, sourceConfiguration), RequestLogEventsSubscriber.class);
    }
}
