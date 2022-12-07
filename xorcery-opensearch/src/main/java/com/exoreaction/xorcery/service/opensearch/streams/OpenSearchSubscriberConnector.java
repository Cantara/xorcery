package com.exoreaction.xorcery.service.opensearch.streams;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.opensearch.api.IndexCommit;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.search.SearchQuery;
import com.exoreaction.xorcery.service.opensearch.client.search.SearchRequest;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class OpenSearchSubscriberConnector {

    private static final Logger logger = LogManager.getLogger(OpenSearchSubscriberConnector.class);

    private ReactiveStreamsClient reactiveStreams;
    private Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher;
    private OpenSearchClient client;

    public OpenSearchSubscriberConnector(OpenSearchClient client, ReactiveStreamsClient reactiveStreams, Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher) {
        this.client = client;
        this.reactiveStreams = reactiveStreams;
        this.openSearchCommitPublisher = openSearchCommitPublisher;
    }

    public void connect(String authority, String streamName, final Configuration publisherConfiguration, Configuration subscriberConfiguration) {

        String indexOrAliasName = subscriberConfiguration.getString("alias")
                .orElseGet(() -> subscriberConfiguration.getString("index").orElseThrow());
        // Check if we already have written data for this index/alias before
        // TODO This should retry in loop on fail
        client.search().search(indexOrAliasName, new SearchRequest.Builder()
                .query(SearchQuery.match_all())
                .build(), Map.of("size", "1", "sort", "@timestamp:desc")).thenApply(response ->
        {
            Configuration.Builder builder = publisherConfiguration.asBuilder();
            response.hits().documents().stream().findFirst().ifPresent(document ->
            {
                document.id().ifPresent(id ->
                {
                    builder.add("id", id);
                });
                builder.add("timestamp", document.timestamp());

                if (document.json().path("_source").path("metadata") instanceof ObjectNode objectNode)
                    builder.builder().setAll(objectNode);
            });
            return builder.build();
        }).whenComplete((updatedConfiguration, throwable) ->
        {
            if (throwable != null) {
                reactiveStreams.subscribe(authority, streamName,
                        () -> publisherConfiguration, new OpenSearchSubscriber(client, openSearchCommitPublisher, subscriberConfiguration), OpenSearchSubscriber.class, Configuration.empty());
            } else {
                reactiveStreams.subscribe(authority, streamName,
                        () -> updatedConfiguration, new OpenSearchSubscriber(client, openSearchCommitPublisher, subscriberConfiguration), OpenSearchSubscriber.class, Configuration.empty());
            }
        });
    }
}
