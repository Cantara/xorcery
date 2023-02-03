package com.exoreaction.xorcery.service.reactivestreams.client;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Flow;

class SubscriberConverter implements Flow.Subscriber<Object> {
    private final Flow.Subscriber<Object> subscriber;
    private MessageWriter<Object> writer;
    private MessageReader<Object> reader;
    private Flow.Subscription subscription;

    public SubscriberConverter(Flow.Subscriber<Object> subscriber, MessageWriter<Object> writer, MessageReader<Object> reader) {
        this.subscriber = subscriber;
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
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
