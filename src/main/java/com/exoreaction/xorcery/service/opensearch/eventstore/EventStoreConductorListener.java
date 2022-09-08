package com.exoreaction.xorcery.service.opensearch.eventstore;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.eventstore.domainevents.EventStoreSubscriber;
import com.exoreaction.xorcery.service.opensearch.eventstore.domainevents.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams2;
import com.exoreaction.xorcery.util.Listeners;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventStoreConductorListener extends AbstractConductorListener {

    private static final Logger logger = LogManager.getLogger(EventStoreConductorListener.class);

    private ReactiveStreams2 reactiveStreams;
    private Listeners<ProjectionListener> listeners;
    private OpenSearchClient client;

    public EventStoreConductorListener(OpenSearchClient client, ReactiveStreams2 reactiveStreams, ServiceIdentifier serviceIdentifier, String rel, Listeners<ProjectionListener> listeners) {
        super(serviceIdentifier, rel);
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.listeners = listeners;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {

        if (consumerConfiguration.getString("type").orElseThrow().equals("domainevents"))
        {
            String indexName = consumerConfiguration.getString("index").orElseThrow();

            // Check if we already have written data for this stream before
//            client.search(Requests.searchRequest(indexName), RequestOptions.DEFAULT).getHits().getHits()

            reactiveStreams.subscribe(link.getHrefAsUri(), sourceConfiguration, new EventStoreSubscriber(consumerConfiguration, indexName, client, listeners), EventStoreSubscriber.class);
        }
    }
}
