/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.eventstore.streams;

import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
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