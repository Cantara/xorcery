package com.exoreaction.xorcery.service.opensearch.streams;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.opensearch.api.IndexCommit;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.document.BulkResponse;
import com.exoreaction.xorcery.service.opensearch.client.document.BulkResponseItem;
import com.exoreaction.xorcery.service.opensearch.client.document.IndexBulkRequest;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class OpenSearchEventHandler
        implements EventHandler<WithMetadata<JsonNode>> {

    private final OpenSearchClient client;
    private Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher;
    private final String index;
    private Logger logger = LogManager.getLogger(getClass());

    private Flow.Subscription subscription;

    private IndexBulkRequest.Builder bulkRequest;
    private long lastTimestamp; // To ensure we don't reuse id's
    private int requestCount = 0;

    public OpenSearchEventHandler(OpenSearchClient client,
                                  Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher,
                                  Flow.Subscription subscription,
                                  String index) {
        this.client = client;
        this.openSearchCommitPublisher = openSearchCommitPublisher;
        this.index = index;
        this.subscription = subscription;
    }

    @Override
    public void onEvent(WithMetadata<JsonNode> event, long sequence, boolean endOfBatch) throws Exception {

        DomainEventMetadata dem = new DomainEventMetadata(event.metadata());

        ObjectNode document = event.metadata().metadata().objectNode();
        document.set("@timestamp", event.metadata().getJson("timestamp").orElse(document.numberNode(0L)));
        document.set("metadata", event.metadata().metadata());
        document.set("data", event.event());

        if (bulkRequest == null)
            bulkRequest = new IndexBulkRequest.Builder();

        // TODO allow event to carry the id in metadata
        long eventTimestamp = dem.getTimestamp();
        if (eventTimestamp <= lastTimestamp)
        {
            eventTimestamp = lastTimestamp + 1;
        }
        String eventId = dem.getHost() + eventTimestamp;
        lastTimestamp = eventTimestamp;
        bulkRequest.create(eventId, document);
        requestCount++;

        if (endOfBatch) {
            LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(dem.getTimestamp()), ZoneId.systemDefault());
            String indexName = String.format(index, date);

            BulkResponse bulkResponse = client.documents().bulk(indexName, bulkRequest.build())
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);

            if (bulkResponse.hasErrors()) {
                LogManager.getLogger(getClass()).error("OpenSearch update errors:\n" + bulkResponse.json().toPrettyString());
            } else {
                bulkResponse.getObjectListAs("items", BulkResponseItem::new).ifPresent(items ->
                {
                    openSearchCommitPublisher.accept(new WithMetadata<>(new OpenSearchMetadata.Builder(new Metadata.Builder())
                            .timestamp(dem.getTimestamp())
                            .build().context(), new IndexCommit(indexName, items.get(items.size() - 1).getSequenceNr())));
                });
            }

            subscription.request(requestCount);
            bulkRequest = null;
            requestCount = 0;
        }
    }
}
