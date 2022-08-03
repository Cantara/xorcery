package com.exoreaction.xorcery.service.opensearch.metrics;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.metrics.MetricsMetadata;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.document.BulkResponse;
import com.exoreaction.xorcery.service.opensearch.client.document.IndexBulkRequest;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import org.apache.logging.log4j.LogManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricEventsSubscriber
        implements ReactiveEventStreams.Subscriber<ObjectNode>, EventHandler<Event<ObjectNode>> {
    private OpenSearchClient client;
    private final ScheduledExecutorService scheduledExecutorService;
    private ReactiveEventStreams.Subscription subscription;
    private final long delay;

    private IndexBulkRequest.Builder bulkRequest;

    public MetricEventsSubscriber(OpenSearchClient client, Configuration consumerConfiguration, Configuration sourceConfiguration, ScheduledExecutorService scheduledExecutorService) {
        this.client = client;

        this.scheduledExecutorService = scheduledExecutorService;
        this.delay = Duration.parse(consumerConfiguration.getString("delay").orElse("PT5S")).toSeconds();
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

        ObjectNode document = event.metadata.metadata().objectNode();
        document.set("@timestamp", event.metadata.getJsonNode("timestamp").orElse(document.numberNode(0L)));
        document.set("metadata", event.metadata.metadata());
        document.set("data", event.event);

        if (bulkRequest == null)
            bulkRequest = new IndexBulkRequest.Builder();

        bulkRequest.create(mmd.getHost()+mmd.getTimestamp(), document);

        if (endOfBatch)
        {
            LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(mmd.getTimestamp()), ZoneId.systemDefault());
            String indexName = "metrics-"+mmd.getEnvironment()+"-"+mmd.getTag()+"-"+ date.getYear()+"-"+date.getMonthValue();
            BulkResponse bulkResponse = client.documents().bulk(indexName, bulkRequest.build())
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);

            if (bulkResponse.hasErrors())
            {
                LogManager.getLogger(getClass()).error("OpenSearch update errors:\n"+bulkResponse.json().toPrettyString());
            }

            bulkRequest = null;
        }

        scheduledExecutorService.schedule(() -> subscription.request(1), delay, TimeUnit.SECONDS);
    }
}