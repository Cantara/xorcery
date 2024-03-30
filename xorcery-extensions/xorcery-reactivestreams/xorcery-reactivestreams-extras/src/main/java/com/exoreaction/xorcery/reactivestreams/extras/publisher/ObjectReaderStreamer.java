package com.exoreaction.xorcery.reactivestreams.extras.publisher;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import reactor.core.publisher.FluxSink;

import java.io.IOException;

public class ObjectReaderStreamer<T> {
    private final FluxSink<T> sink;
    private final YAMLParser parser;
    private final ObjectReader objectReader;
    private long skip;

    public ObjectReaderStreamer(FluxSink<T> sink, YAMLParser parser, ObjectReader objectReader) {
        this.sink = sink;
        this.parser = parser;
        this.objectReader = objectReader;

        // Skip until position
        if (sink.contextView().getOrDefault("position", null) instanceof Long pos) {
            this.skip = pos + 1;
        } else {
            this.skip = 0;
        }

        sink.onRequest(this::request);
    }

    private void request(long request) {
        try {
            JsonToken token = null;
            while (request-- > 0 && !(token = parser.nextToken()).isStructEnd()) {
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
