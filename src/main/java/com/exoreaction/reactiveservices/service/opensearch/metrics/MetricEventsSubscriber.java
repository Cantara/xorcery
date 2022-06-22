package com.exoreaction.reactiveservices.service.opensearch.metrics;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.RequestMetadata;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.service.metrics.MetricsMetadata;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
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
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentType;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricEventsSubscriber
        implements ReactiveEventStreams.Subscriber<ObjectNode>, EventHandler<Event<ObjectNode>> {
    private RestHighLevelClient client;
    private final ScheduledExecutorService scheduledExecutorService;
    private ReactiveEventStreams.Subscription subscription;
    private ObjectMapper objectMapper;
    private final long delay;

    private BulkRequest bulkRequest;

    public MetricEventsSubscriber(RestHighLevelClient client, Configuration consumerConfiguration, Configuration sourceConfiguration, ScheduledExecutorService scheduledExecutorService) {
        this.client = client;

        this.scheduledExecutorService = scheduledExecutorService;
        this.delay = Duration.parse(consumerConfiguration.getString("delay").orElse("PT5S")).toSeconds();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public EventSink<Event<ObjectNode>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        this.subscription = subscription;

        Disruptor<Event<ObjectNode>> disruptor = new Disruptor<>(Event::new, 1024, new NamedThreadFactory("OpenSearchMetrics-"));
        disruptor.handleEventsWith(this);
        disruptor.start();

        scheduledExecutorService.schedule(() -> subscription.request(1), delay, TimeUnit.SECONDS);

        return disruptor.getRingBuffer();
    }

    @Override
    public void onEvent(Event<ObjectNode> event, long sequence, boolean endOfBatch) throws Exception {

        MetricsMetadata mmd = new MetricsMetadata(event.metadata);
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(mmd.getTimestamp()), ZoneId.systemDefault());
        String indexName = "metrics-"+mmd.getEnvironment()+"-"+mmd.getTag()+"-"+mmd.getName()+"-"+ date.getYear()+""+date.getMonthValue();

        IndexRequest request = new IndexRequest(indexName); //Add a document to the custom-index we created.
        request.id(mmd.getHost()+mmd.getTimestamp()); //Assign an ID to the document.

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

        scheduledExecutorService.schedule(() -> subscription.request(1), delay, TimeUnit.SECONDS);
    }
}