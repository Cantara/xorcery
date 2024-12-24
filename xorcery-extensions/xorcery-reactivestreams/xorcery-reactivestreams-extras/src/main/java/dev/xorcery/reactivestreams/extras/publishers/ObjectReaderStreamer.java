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
