package com.exoreaction.reactiveservices.service.opensearch.metrics;

import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.api.AbstractConductorListener;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.RestHighLevelClient;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class MetricsConductorListener extends AbstractConductorListener {
    private RestHighLevelClient client;
    private ReactiveStreams reactiveStreams;
    private ScheduledExecutorService scheduledExecutorService;
    private final ServiceIdentifier serviceIdentifier;
    private final String rel;

    public MetricsConductorListener(RestHighLevelClient client, ReactiveStreams reactiveStreams,
                                    ScheduledExecutorService scheduledExecutorService,
                                    ServiceIdentifier serviceIdentifier, String rel) {
        super(serviceIdentifier, rel);
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.scheduledExecutorService = scheduledExecutorService;
        this.serviceIdentifier = serviceIdentifier;
        this.rel = rel;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Optional<ObjectNode> sourceParameters, Optional<ObjectNode> consumerParameters) {
        reactiveStreams.subscribe(serviceIdentifier, link, new MetricEventsSubscriber(client, consumerParameters, sourceParameters, scheduledExecutorService), sourceParameters);
    }
}
