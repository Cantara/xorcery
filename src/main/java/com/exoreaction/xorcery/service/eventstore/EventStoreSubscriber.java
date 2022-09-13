package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.eventstore.disruptor.EventStoreSubscriberEventHandler;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public class EventStoreSubscriber
        implements Flow.Subscriber<WithResult<WithMetadata<ByteBuffer>, Metadata>> {

    private Disruptor<WithResult<WithMetadata<ByteBuffer>, Metadata>> disruptor;
    private EventStoreDBClient client;
    private Configuration cfg;

    public EventStoreSubscriber(EventStoreDBClient client, Configuration cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        disruptor = new Disruptor<>(WithResult::new, 512, new NamedThreadFactory("EventStoreSubscriber-"),
                ProducerType.SINGLE,
                new BlockingWaitStrategy());
        disruptor.handleEventsWith(new EventStoreSubscriberEventHandler(client, subscription));
        disruptor.start();
        subscription.request(512);
    }

    @Override
    public void onNext(WithResult<WithMetadata<ByteBuffer>, Metadata> item) {
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