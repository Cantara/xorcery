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
package dev.xorcery.opensearch.client.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.opensearch.OpenSearchConfiguration;
import dev.xorcery.opensearch.client.OpenSearchClient;
import dev.xorcery.opensearch.client.OpenSearchContext;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import dev.xorcery.reactivestreams.extras.operators.SmartBatchingOperator;
import io.opentelemetry.api.OpenTelemetry;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.opentelemetry.context.Context.taskWrapping;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class DocumentUpdates
        implements BiFunction<Flux<MetadataJsonNode<JsonNode>>, ContextView, Publisher<MetadataJsonNode<JsonNode>>> {

    private final OpenSearchConfiguration configuration;
    private final OpenSearchClient.Factory clientFactory;
    private final Function<MetadataJsonNode<JsonNode>, String> documentIdSelector;
    private final Function<ObjectNode, ObjectNode> jsonSelector;
    private Logger logger;
    private final OpenTelemetry openTelemetry;

    public DocumentUpdates(
            OpenSearchConfiguration configuration,
            OpenSearchClient.Factory clientFactory,
            Function<MetadataJsonNode<JsonNode>, String> documentIdSelector,
            Function<ObjectNode, ObjectNode> jsonSelector, Logger logger,
            OpenTelemetry openTelemetry) {
        this.configuration = configuration;
        this.clientFactory = clientFactory;
        this.documentIdSelector = documentIdSelector;
        this.jsonSelector = jsonSelector;
        this.logger = logger;
        this.openTelemetry = openTelemetry;
    }

    @Override
    public Publisher<MetadataJsonNode<JsonNode>> apply(Flux<MetadataJsonNode<JsonNode>> flux, ContextView contextView) {

        ContextViewElement cve = new ContextViewElement(contextView);
        URI host = cve.getURI(OpenSearchContext.serverUri).orElseGet(configuration::getURI);
        OpenSearchClient client = clientFactory.create(host);
        String index = cve.getString("index").orElse(null);
        // TODO Implement this                .contextWrite(setStreamMetadata())
        return flux.transformDeferredContextual(SmartBatchingOperator.smartBatching(
                handler(client.documents(), index),
                () -> new ArrayBlockingQueue<>(1024),
                () -> taskWrapping(Schedulers.boundedElastic()::schedule)));
    }

    protected Function<Context, Context> addLastPosition() {
        return context ->
        {
/* TODO Replace with OpenSearch equivalent of last known position
            try {
                String streamId = new ContextViewElement(context).getString(ReactiveStreamsContext.streamId)
                        .orElseThrow(missing(ReactiveStreamsContext.streamId));
                return client.readStream(streamId, ReadStreamOptions.get().backwards().maxCount(1))
                        .orTimeout(10, TimeUnit.SECONDS).thenApply(readResult ->
                        {
                            long lastStreamPosition = readResult.getLastStreamPosition();
                            return context.put(ReactiveStreamsContext.streamPosition, lastStreamPosition);
                        }).exceptionallyCompose(throwable ->
                        {
                            if (throwable.getCause() instanceof StreamNotFoundException) {
                                return CompletableFuture.completedStage(context);
                            } else {
                                return CompletableFuture.failedStage(throwable.getCause());
                            }
                        }).orTimeout(10, TimeUnit.SECONDS).join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
*/
            return context;
        };
    }

    private BiConsumer<Collection<MetadataJsonNode<JsonNode>>, SynchronousSink<Collection<MetadataJsonNode<JsonNode>>>> handler(DocumentClient client, String index) {
        return (metadataJsonNodes, sink) -> {

            IndexBulkRequest.Builder bulkRequest = new IndexBulkRequest.Builder();

            for (MetadataJsonNode<JsonNode> item : metadataJsonNodes) {
                Metadata metadata = item.metadata();
                ObjectNode document = metadata.metadata().objectNode();
                JsonNode timestamp = metadata.getJson("timestamp").orElseGet(() -> document.numberNode(System.currentTimeMillis()));

                document.set("@timestamp", timestamp);
                document.set("metadata", metadata.metadata());
                document.set("data", item.data());

                String itemIndexName = metadata.metadata().remove("index") instanceof TextNode indexJson ? indexJson.textValue() : null;
                String effectiveIndexName = itemIndexName != null ? itemIndexName : index;

                // TODO allow item to carry the id in metadata
                String eventId = documentIdSelector.apply(item);
                bulkRequest.create(effectiveIndexName, eventId, jsonSelector.apply(document));
            }

            BulkResponse bulkResponse = client.bulk(bulkRequest.build())
                    .toCompletableFuture().orTimeout(10, TimeUnit.SECONDS).join();
            // TODO add index and revision to metadata
            if (bulkResponse.hasErrors()) {
                sink.error(new BulkUpdateException("OpenSearch update errors:\n" + bulkResponse.json().toPrettyString()));
            } else {
                sink.next(metadataJsonNodes);
            }
        };
    }
}
