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

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.opensearch.api.IndexCommit;
import com.exoreaction.xorcery.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.lmax.disruptor.dsl.Disruptor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.util.context.Context;

import java.util.function.Consumer;

public class OpenSearchSubscriber
        extends BaseSubscriber<WithMetadata<JsonNode>> {

    private final OpenSearchClient client;
    private final String indexName;
    private final Configuration publisherConfiguration;
    private Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher;
    private Configuration configuration;

    private Disruptor<WithMetadata<JsonNode>> disruptor;

    public OpenSearchSubscriber(OpenSearchClient client, Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher, Configuration configuration, Configuration publisherConfiguration) {
        this.client = client;
        this.openSearchCommitPublisher = openSearchCommitPublisher;
        this.configuration = configuration;
        this.indexName = configuration.getString("index").orElseThrow();
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public Context currentContext() {
        return Context.of(JsonElement.toMap(publisherConfiguration.json(), JsonNode::asText));
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        disruptor = new Disruptor<>(WithMetadata::new, configuration.getInteger("bufferSize").orElse(64), new NamedThreadFactory("OpenSearch-" + indexName + "-"));
        disruptor.handleEventsWith(new OpenSearchEventHandler(client, openSearchCommitPublisher, subscription, indexName));
        disruptor.start();

        subscription.request(disruptor.getBufferSize());
    }

    @Override
    protected void hookOnNext(WithMetadata<JsonNode> item) {
        disruptor.publishEvent((e, s, event) ->
        {
            e.set(event);
        }, item);
    }

    @Override
    protected void hookOnComplete() {
        disruptor.shutdown();
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        disruptor.shutdown();
    }
}