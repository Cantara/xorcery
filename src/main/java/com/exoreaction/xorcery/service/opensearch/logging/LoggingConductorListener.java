package com.exoreaction.xorcery.service.opensearch.logging;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;

public class LoggingConductorListener extends AbstractConductorListener {
    private OpenSearchClient client;
    private ReactiveStreams reactiveStreams;
    private final ServiceIdentifier serviceIdentifier;
    private final String rel;

    public LoggingConductorListener(OpenSearchClient client, ReactiveStreams reactiveStreams,
                                    ServiceIdentifier serviceIdentifier, String rel) {
        super(serviceIdentifier, rel);
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.serviceIdentifier = serviceIdentifier;
        this.rel = rel;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
        reactiveStreams.subscribe(serviceIdentifier, link, new LoggingEventsSubscriber(client, consumerConfiguration, sourceConfiguration), sourceConfiguration, Configuration.empty());
    }
}
