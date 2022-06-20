package com.exoreaction.reactiveservices.service.opensearch.eventstore;

import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.api.AbstractConductorListener;
import com.exoreaction.reactiveservices.service.opensearch.eventstore.domainevents.EventStoreSubscriber;
import com.exoreaction.reactiveservices.service.opensearch.eventstore.domainevents.ProjectionListener;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.util.Listeners;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.client.RestHighLevelClient;

import java.util.Optional;

public class EventStoreConductorListener extends AbstractConductorListener {

    private Logger logger = LogManager.getLogger(getClass());

    private ReactiveStreams reactiveStreams;
    private Listeners<ProjectionListener> listeners;
    private RestHighLevelClient client;

    public EventStoreConductorListener(RestHighLevelClient client, ReactiveStreams reactiveStreams, ServiceIdentifier serviceIdentifier, String rel, Listeners<ProjectionListener> listeners) {
        super(serviceIdentifier, rel);
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.listeners = listeners;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Optional<ObjectNode> sourceAttributes, Optional<ObjectNode> consumerAttributes) {

        ObjectNode attrs = consumerAttributes.orElseThrow();
        if (attrs.path("type").textValue().equals("domainevents"))
        {
            String indexName = attrs.path("index").textValue();

            // Check if we already have written data for this stream before
//            client.search(Requests.searchRequest(indexName), RequestOptions.DEFAULT).getHits().getHits()

            reactiveStreams.subscribe(sro.serviceIdentifier(), link, new EventStoreSubscriber(consumerAttributes, indexName, client, listeners), sourceAttributes);
        }
    }
}
