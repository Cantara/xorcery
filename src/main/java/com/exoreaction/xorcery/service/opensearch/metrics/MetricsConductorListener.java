package com.exoreaction.xorcery.service.opensearch.metrics;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;

import java.util.concurrent.ScheduledExecutorService;

public class MetricsConductorListener extends AbstractConductorListener {
    private final OpenSearchClient client;
    private final ReactiveStreams reactiveStreams;
    private final ScheduledExecutorService scheduledExecutorService;

    public MetricsConductorListener(OpenSearchClient client, ReactiveStreams reactiveStreams,
                                    ScheduledExecutorService scheduledExecutorService,
                                    ServiceIdentifier serviceIdentifier, String rel) {
        super(serviceIdentifier, rel);
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
        reactiveStreams.subscribe(link.getHrefAsUri(), sourceConfiguration, new MetricEventsSubscriber(client, consumerConfiguration, scheduledExecutorService), MetricEventsSubscriber.class);
    }
}
