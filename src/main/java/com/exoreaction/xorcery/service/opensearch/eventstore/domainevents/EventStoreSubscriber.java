package com.exoreaction.xorcery.service.opensearch.eventstore.domainevents;

import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.util.Listeners;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class EventStoreSubscriber
        implements ReactiveEventStreams.Subscriber<ArrayNode> {

    private Disruptor<Event<ArrayNode>> disruptor;
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
    public EventSink<Event<ArrayNode>> onSubscribe(ReactiveEventStreams.Subscription subscription, Configuration configuration) {
        disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("OpenSearchEventStoreDomainEventsDisruptorIn-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new OpenSearchEventStoreEventHandler(client, subscription, indexName, listeners));
        disruptor.start();
        subscription.request(4096);
        return disruptor.getRingBuffer();
    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
    }
}
