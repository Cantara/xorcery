package com.exoreaction.xorcery.service.opensearch.requestlog;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.log4jappender.LoggingMetadata;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.document.BulkResponse;
import com.exoreaction.xorcery.service.opensearch.client.document.IndexBulkRequest;
import com.exoreaction.xorcery.service.reactivestreams.api.Subscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.Subscription;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

public class RequestLogEventsSubscriber
        implements Flow.Subscriber<WithMetadata<ObjectNode>>, EventHandler<WithMetadata<ObjectNode>> {
    private OpenSearchClient client;

    private Flow.Subscription subscription;

    private IndexBulkRequest.Builder bulkRequest;
    private Disruptor<WithMetadata<ObjectNode>> disruptor;

    public RequestLogEventsSubscriber(OpenSearchClient client, Configuration consumerConfiguration, Configuration sourceConfiguration) {
        this.client = client;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;

        disruptor = new Disruptor<>(WithMetadata::new, 1024, new NamedThreadFactory("OpenSearchRequestLog-"));
        disruptor.handleEventsWith(this);
        disruptor.start();

        subscription.request(1024);
    }

    @Override
    public void onNext(WithMetadata<ObjectNode> item) {
        disruptor.publishEvent((e, s, event) ->
        {
            e.set(event);
        }, item);
    }

    @Override
    public void onError(Throwable throwable) {
        disruptor.shutdown();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }

    @Override
    public void onEvent(WithMetadata<ObjectNode> event, long sequence, boolean endOfBatch) throws Exception {

        LoggingMetadata lmd = new LoggingMetadata(event.metadata());

        ObjectNode document = event.metadata().metadata().objectNode();
        document.set("@timestamp", event.metadata().getJsonNode("timestamp").orElse(document.numberNode(0L)));
        document.set("metadata", event.metadata().metadata());
        document.set("data", event.event());

        if (bulkRequest == null)
            bulkRequest = new IndexBulkRequest.Builder();

        bulkRequest.create(null, document);

        if (endOfBatch) {
            LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(lmd.getTimestamp()), ZoneId.systemDefault());
            String indexName = "requestlogs-" + lmd.getEnvironment() + "-" + lmd.getTag() + "-" + date.getYear() + "-" + date.getMonthValue();

            BulkResponse bulkResponse = client.documents().bulk(indexName, bulkRequest.build())
                    .toCompletableFuture().get(10, TimeUnit.SECONDS);

            if (bulkResponse.hasErrors()) {
                LogManager.getLogger(getClass()).error("OpenSearch update errors:\n" + bulkResponse.json().toPrettyString());
            }

            bulkRequest = null;
        }

        subscription.request(1);
    }
}