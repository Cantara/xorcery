package com.exoreaction.xorcery.service.opensearch.streams;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.opensearch.api.IndexCommit;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class OpenSearchSubscriber
        implements Flow.Subscriber<WithMetadata<JsonNode>> {

    private final OpenSearchClient client;
    private final String indexName;
    private Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher;
    private Configuration configuration;

    private Disruptor<WithMetadata<JsonNode>> disruptor;

    public OpenSearchSubscriber(OpenSearchClient client, Consumer<WithMetadata<IndexCommit>> openSearchCommitPublisher, Configuration configuration) {
        this.client = client;
        this.openSearchCommitPublisher = openSearchCommitPublisher;
        this.configuration = configuration;
        this.indexName = configuration.getString("index").orElseThrow();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {

        disruptor = new Disruptor<>(WithMetadata::new, configuration.getInteger("bufferSize").orElse(64), new NamedThreadFactory("OpenSearch-" + indexName + "-"));
        disruptor.handleEventsWith(new OpenSearchEventHandler(client, openSearchCommitPublisher, subscription, indexName));
        disruptor.start();

        subscription.request(disruptor.getBufferSize());
    }

    @Override
    public void onNext(WithMetadata<JsonNode> item) {
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
}