package com.exoreaction.xorcery.reactivestreams.extras.publishers;

import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.ReactiveStreamsContext;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.FluxSink;

import java.io.IOException;

class ObjectReaderStreamer<T>
        implements Subscription {
    private final JsonParser parser;
    private final ObjectReader objectReader;
    private final CoreSubscriber<T> subscriber;
    private long skip;

    public ObjectReaderStreamer(CoreSubscriber<T> subscriber, JsonParser parser, ObjectReader objectReader) {
        this.subscriber = subscriber;
        this.parser = parser;
        this.objectReader = objectReader;

        // Skip until position
        this.skip = new ContextViewElement(subscriber.currentContext())
                .getLong(ReactiveStreamsContext.streamPosition)
                .map(pos -> pos + 1).orElse(0L);
    }

    public void request(long request) {
        try {
            JsonToken token = null;
            while (request-- > 0 && (token = parser.nextToken()) != null && !token.isStructEnd()) {
                //                    System.out.println(token);
                parser.nextToken();
                T item = objectReader.readValue(parser);
                if (skip > 0) {
                    request++;
                    skip--;
                } else {
                    subscriber.onNext(item);
                }
            }

            if (token == null || token.isStructEnd()) {
                subscriber.onComplete();
            }
        } catch (Throwable e) {
            subscriber.onError(e);
        }
    }

    @Override
    public void cancel() {
        try {
            parser.close();
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }
}
