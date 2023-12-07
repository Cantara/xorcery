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
package com.exoreaction.xorcery.reactivestreams.util;

import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Processor that converts items from one type to another using specified MessageWriter/MessageReader.
 *
 * @param <P>
 * @param <S>
 */
public class TypeConverterProcessor<P, S>
        implements Processor<P, S> {
    private final MessageWriter<P> writer;
    private final MessageReader<S> reader;
    private Subscriber<? super S> subscriber;
    private Subscription subscription;

    public TypeConverterProcessor(MessageWriter<P> writer, MessageReader<S> reader) {
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    public void subscribe(Subscriber<? super S> subscriber) {
        this.subscriber = subscriber;
        if (subscription != null)
            subscriber.onSubscribe(subscription);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        if (subscriber != null)
            subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(P item) {

        // Convert item to correct type
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writer.writeTo(item, bout);
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            S newItem = reader.readFrom(bin);
            subscriber.onNext(newItem);
        } catch (IOException e) {
            subscriber.onError(e);
            subscription.cancel();
        }
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
