package com.exoreaction.xorcery.service.opensearch.eventstore.domainevents;

import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.document.BulkResponse;
import com.exoreaction.xorcery.service.opensearch.client.document.IndexBulkRequest;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.util.Listeners;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class OpenSearchEventStoreEventHandler
        implements EventHandler<WithMetadata<ArrayNode>> {

    private final OpenSearchClient client;
    private final String indexName;
    private final ObjectMapper objectMapper;
    private Logger logger = LogManager.getLogger(getClass());

    private Flow.Subscription subscription;
    private Listeners<ProjectionListener> listeners;

    private IndexBulkRequest.Builder bulkRequest;
    private int requestCount = 0;

    public OpenSearchEventStoreEventHandler(OpenSearchClient client,
                                            Flow.Subscription subscription,
                                            String indexName,
                                            Listeners<ProjectionListener> listeners) {
        this.client = client;
        this.indexName = indexName;
        this.subscription = subscription;
        this.listeners = listeners;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onEvent(WithMetadata<ArrayNode> event, long sequence, boolean endOfBatch) throws Exception {

        DomainEventMetadata dem = new DomainEventMetadata(event.metadata());

        ObjectNode document = event.metadata().metadata().objectNode();
        document.set("@timestamp", event.metadata().getJsonNode("timestamp").orElse(document.numberNode(0L)));
        document.set("metadata", event.metadata().metadata());
        document.set("data", event.event());

        if (bulkRequest == null)
            bulkRequest = new IndexBulkRequest.Builder();

        bulkRequest.create(dem.getHost()+dem.getTimestamp(), document);
        requestCount++;

        if (endOfBatch)
        {
            BulkResponse bulkResponse = client.documents().bulk(indexName, bulkRequest.build())
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);

            if (bulkResponse.hasErrors())
            {
                LogManager.getLogger(getClass()).error("OpenSearch update errors:\n"+bulkResponse.json().toPrettyString());
            }

            subscription.request(requestCount);
            bulkRequest = null;
            requestCount = 0;

//            listeners.listener().onCommit(indexName, );
        }
    }
}
