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
package com.exoreaction.xorcery.reactivestreams.client;

import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

class SubscriberConverter implements Subscriber<Object> {
    private final Subscriber<Object> subscriber;
    private MessageWriter<Object> writer;
    private MessageReader<Object> reader;
    private Subscription subscription;

    public SubscriberConverter(Subscriber<Object> subscriber, MessageWriter<Object> writer, MessageReader<Object> reader) {
        this.subscriber = subscriber;
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(Object item) {

        // Convert item to correct type
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writer.writeTo(item, bout);
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            item = reader.readFrom(bin);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
