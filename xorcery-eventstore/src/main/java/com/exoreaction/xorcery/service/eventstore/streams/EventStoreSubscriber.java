package com.exoreaction.xorcery.service.eventstore.streams;

import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public class EventStoreSubscriber
        implements Flow.Subscriber<WithMetadata<ByteBuffer>> {

    private Disruptor<WithMetadata<ByteBuffer>> disruptor;
    private EventStoreDBClient client;
    private Configuration cfg;

    public EventStoreSubscriber(EventStoreDBClient client, Configuration cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        disruptor = new Disruptor<>(WithMetadata::new, 512, new NamedThreadFactory("EventStoreSubscriber-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new EventStoreSubscriberEventHandler(client, subscription, cfg.getString("stream")));
        disruptor.start();
        subscription.request(512);
    }

    @Override
    public void onNext(WithMetadata<ByteBuffer> item) {
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