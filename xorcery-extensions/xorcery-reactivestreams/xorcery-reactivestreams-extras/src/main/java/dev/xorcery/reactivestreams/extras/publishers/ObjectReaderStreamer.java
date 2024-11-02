package dev.xorcery.reactivestreams.extras.publishers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

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

            if (request == 0)
                return;

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
                parser.close();
                subscriber.onComplete();
            }
        } catch (Throwable e) {
            try {
                parser.close();
            } catch (IOException ex) {
                // Ignore
            }
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
