package com.exoreaction.xorcery.reactivestreams.extras.publishers;

import com.exoreaction.xorcery.reactivestreams.api.reactor.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ReactiveStreamsContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import reactor.core.publisher.FluxSink;

import java.io.IOException;

class ObjectReaderStreamer<T> {
    private final FluxSink<T> sink;
    private final YAMLParser parser;
    private final ObjectReader objectReader;
    private long skip;

    public ObjectReaderStreamer(FluxSink<T> sink, YAMLParser parser, ObjectReader objectReader) {
        this.sink = sink;
        this.parser = parser;
        this.objectReader = objectReader;

        // Skip until position
        this.skip = new ContextViewElement(sink.contextView())
                .getLong(ReactiveStreamsContext.streamPosition)
                .map(pos -> pos + 1).orElse(0L);

        sink.onRequest(this::request);
    }

    private void request(long request) {
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
                    sink.next(item);
                }
            }

            if (token == null || token.isStructEnd()) {
                sink.complete();
            }
        } catch (IOException e) {
            sink.error(e);
        }
    }
}
