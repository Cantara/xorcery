package com.exoreaction.reactiveservices.service.opensearch.eventstore.domainevents;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.util.Listeners;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class OpenSearchEventStoreEventHandler
        implements DefaultEventHandler<Event<ArrayNode>> {

    private final RestHighLevelClient client;
    private final String indexName;
    private final ObjectMapper objectMapper;
    private Logger logger = LogManager.getLogger(getClass());

    private ReactiveEventStreams.Subscription subscription;
    private Listeners<ProjectionListener> listeners;

    private BulkRequest bulkRequest;

    public OpenSearchEventStoreEventHandler(RestHighLevelClient client,
                                            ReactiveEventStreams.Subscription subscription,
                                            String indexName,
                                            Listeners<ProjectionListener> listeners) {
        this.client = client;
        this.indexName = indexName;
        this.subscription = subscription;
        this.listeners = listeners;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void onEvent(Event<ArrayNode> event, long sequence, boolean endOfBatch) throws Exception {

        DomainEventMetadata dem = new DomainEventMetadata(event.metadata);

        IndexRequest request = new IndexRequest(indexName); //Add a document to the custom-index we created.
        request.id(dem.getHost()+dem.getTimestamp()); //Assign an ID to the document.

        ObjectNode document = event.metadata.metadata().objectNode();
        document.set("@timestamp", event.metadata.getJsonNode("timestamp").orElse(document.numberNode(0L)));
        document.set("metadata", event.metadata.metadata());
        document.set("data", event.event);
        byte[] data = objectMapper.writeValueAsBytes(document);
        request.source(data, XContentType.JSON); //Place your content into the index's source.

        if (bulkRequest == null)
            bulkRequest = new BulkRequest();

        bulkRequest.add(request);

        if (endOfBatch)
        {
            BulkResponse indexResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);

            subscription.request(bulkRequest.numberOfActions());
            bulkRequest = null;
        }
    }
}
