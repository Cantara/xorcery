package com.exoreaction.xorcery.service.opensearch.streams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.opensearch.api.IndexCommit;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.search.SearchQuery;
import com.exoreaction.xorcery.service.opensearch.client.search.SearchRequest;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.function.Consumer;

public class ClientSubscriberConductorListener extends AbstractConductorListener {

    private static final Logger logger = LogManager.getLogger(ClientSubscriberConductorListener.class);

    private ReactiveStreams reactiveStreams;
    private Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher;
    private OpenSearchClient client;

    public ClientSubscriberConductorListener(OpenSearchClient client, ReactiveStreams reactiveStreams, Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher, ServiceIdentifier serviceIdentifier) {
        super(serviceIdentifier, null);
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.openSearchCommitPublisher = openSearchCommitPublisher;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, final Configuration sourceConfiguration, Configuration consumerConfiguration) {

        String indexName = consumerConfiguration.getString("index").orElseThrow();
        String aliasName = consumerConfiguration.getString("alias")
                .orElseGet(() -> consumerConfiguration.getString("index").orElseThrow());

        // Check if we already have written data for this index/alias before
        // TODO This should retry in loop on fail
        client.search().search(aliasName, new SearchRequest.Builder()
                .query(SearchQuery.match_all())
                .build(), Map.of("size", "1", "sort", "@timestamp:desc")).thenApply(response ->
        {
            Configuration.Builder builder = sourceConfiguration.asBuilder();
            response.hits().documents().stream().findFirst().ifPresent(document ->
            {
                document.id().ifPresent(id ->
                {
                    builder.add("id", id);
                });

                builder.add("timestamp", document.timestamp());
            });
            return builder.build();
        }).whenComplete((updatedConfiguration, throwable) ->
        {
            if (throwable != null) {
                reactiveStreams.subscribe(link.getHrefAsUri(), sourceConfiguration, new OpenSearchSubscriber(client, openSearchCommitPublisher, consumerConfiguration), OpenSearchSubscriber.class);
            } else {
                reactiveStreams.subscribe(link.getHrefAsUri(), updatedConfiguration, new OpenSearchSubscriber(client, openSearchCommitPublisher, consumerConfiguration), OpenSearchSubscriber.class);
            }
        });
    }
}
