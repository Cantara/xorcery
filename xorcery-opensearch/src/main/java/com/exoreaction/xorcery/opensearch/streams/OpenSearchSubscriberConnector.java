/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.opensearch.streams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.opensearch.api.IndexCommit;
import com.exoreaction.xorcery.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.opensearch.client.search.SearchQuery;
import com.exoreaction.xorcery.opensearch.client.search.SearchRequest;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.Map;
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

    public void connect(URI serverUri, String streamName, final Configuration publisherConfiguration, Configuration subscriberConfiguration) {

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
                reactiveStreams.subscribe(serverUri, streamName,
                        () -> publisherConfiguration, new OpenSearchSubscriber(client, openSearchCommitPublisher, subscriberConfiguration), OpenSearchSubscriber.class, ClientConfiguration.defaults());
            } else {
                reactiveStreams.subscribe(serverUri, streamName,
                        () -> updatedConfiguration, new OpenSearchSubscriber(client, openSearchCommitPublisher, subscriberConfiguration), OpenSearchSubscriber.class, ClientConfiguration.defaults());
            }
        });
    }
}
