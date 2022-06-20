package com.exoreaction.reactiveservices.service.opensearch.eventstore.domainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.util.Listeners;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.opensearch.client.RestHighLevelClient;

import java.util.Optional;

public class EventStoreSubscriber
        implements ReactiveEventStreams.Subscriber<ArrayNode> {

    private Disruptor<Event<ArrayNode>> disruptor;
    private Optional<ObjectNode> selfParameters;
    private final String indexName;
    private final RestHighLevelClient client;
    private Listeners<ProjectionListener> listeners;

    public EventStoreSubscriber(Optional<ObjectNode> selfParameters,
                                String indexName,
                                RestHighLevelClient client,
                                Listeners<ProjectionListener> listeners) {

        this.selfParameters = selfParameters;
        this.indexName = indexName;
        this.client = client;
        this.listeners = listeners;
    }

    @Override
    public EventSink<Event<ArrayNode>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
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
