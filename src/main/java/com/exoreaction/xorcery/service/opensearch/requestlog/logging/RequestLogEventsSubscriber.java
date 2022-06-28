package com.exoreaction.xorcery.service.opensearch.requestlog.logging;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class RequestLogEventsSubscriber
        implements ReactiveEventStreams.Subscriber<ObjectNode>, EventHandler<Event<ObjectNode>> {
    private RestHighLevelClient client;
    private ReactiveEventStreams.Subscription subscription;
    private ObjectMapper objectMapper;

    private BulkRequest bulkRequest;

    public RequestLogEventsSubscriber(RestHighLevelClient client, Configuration consumerConfiguration, Configuration sourceConfiguration) {
        this.client = client;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EventSink<Event<ObjectNode>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        this.subscription = subscription;

        Disruptor<Event<ObjectNode>> disruptor = new Disruptor<>(Event::new, 1024, new NamedThreadFactory("OpenSearchRequestLog-"));
        disruptor.handleEventsWith(this);
        disruptor.start();

        subscription.request(1024);

        return disruptor.getRingBuffer();
    }

    @Override
    public void onEvent(Event<ObjectNode> event, long sequence, boolean endOfBatch) throws Exception {

        LoggingMetadata lmd = new LoggingMetadata(event.metadata);
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(lmd.getTimestamp()), ZoneId.systemDefault());
        String indexName = "requests-"+lmd.getEnvironment()+"-"+lmd.getTag()+"-"+lmd.getName()+"-"+ date.getYear()+""+date.getMonthValue();

        IndexRequest request = new IndexRequest(indexName); //Add a document to the custom-index we created.
        request.id(lmd.getHost()+lmd.getTimestamp()); //Assign an ID to the document.

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

            bulkRequest = null;
        }

        subscription.request(1);
    }
}