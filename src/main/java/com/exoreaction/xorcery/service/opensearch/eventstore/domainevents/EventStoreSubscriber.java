package com.exoreaction.xorcery.service.opensearch.eventstore.domainevents;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.util.Listeners;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.Flow;

public class EventStoreSubscriber
        implements Flow.Subscriber<WithMetadata<ArrayNode>> {

    private Disruptor<WithMetadata<ArrayNode>> disruptor;
    private final String indexName;
    private final OpenSearchClient client;
    private Listeners<ProjectionListener> listeners;
    private Configuration consumerConfiguration;

    public EventStoreSubscriber(Configuration consumerConfiguration,
                                String indexName,
                                OpenSearchClient client,
                                Listeners<ProjectionListener> listeners) {
        this.consumerConfiguration = consumerConfiguration;
        this.indexName = indexName;
        this.client = client;
        this.listeners = listeners;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        disruptor = new Disruptor<>(WithMetadata::new, 4096, new NamedThreadFactory("OpenSearchEventStoreDomainEventsDisruptorIn-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new OpenSearchEventStoreEventHandler(client, subscription, indexName, listeners));
        disruptor.start();
        subscription.request(4096);
    }

    @Override
    public void onNext(WithMetadata<ArrayNode> item) {
        disruptor.publishEvent((e, seq, event) ->
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
