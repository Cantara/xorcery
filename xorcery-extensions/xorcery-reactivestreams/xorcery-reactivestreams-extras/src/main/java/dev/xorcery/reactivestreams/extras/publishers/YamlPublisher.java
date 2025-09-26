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

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import dev.xorcery.collections.Element;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.yaml.snakeyaml.LoaderOptions;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.LongConsumer;

/**
 * Put the URL to the resource to be published into the subscriber ContextView with key {@link ResourcePublisherContext#resourceUrl}
 *
 * @param <T>
 */
public class YamlPublisher<T>
        implements Publisher<T> {

    private static final Scheduler scheduler = Schedulers.newSingle("YAML");

    private static final ObjectReader yamlReader = new YAMLMapper().findAndRegisterModules().reader();

    private final Flux<T> flux;

    public YamlPublisher(Class<? super T> itemType) {
        ObjectReader reader = yamlReader.forType(itemType);
        this.flux = Flux.<T>create(sink -> {
            try {
                Object resourceUrl = new ContextViewElement(sink.currentContext()).get(ResourcePublisherContext.resourceUrl)
                        .orElseThrow(Element.missing(ResourcePublisherContext.resourceUrl));
                URL yamlResource = resourceUrl instanceof URL url ? url : new URL(resourceUrl.toString());
                InputStream resourceAsStream = new BufferedInputStream(yamlResource.openStream(), 32 * 1024);
                // Skip until position
                long skip = new ContextViewElement(sink.currentContext())
                        .getLong(ReactiveStreamsContext.streamPosition)
                        .map(pos -> pos + 1).orElse(0L);

                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
                YAMLFactory factory = YAMLFactory.builder().loaderOptions(loaderOptions).build();

                YAMLParser parser = factory.createParser(resourceAsStream);

                sink.onRequest(onRequest(parser, reader, skip, sink));
                sink.onDispose(onDispose(parser));
                sink.onCancel(onDispose(parser));
            } catch (IOException e) {
                sink.error(e);
            }
        }).subscribeOn(scheduler, true);
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        flux.subscribe(s);
    }

    private Disposable onDispose(YAMLParser parser) {
        return () -> {
            try {
                parser.close();
            } catch (IOException e) {
                // Ignore
            }
        };
    }

    private LongConsumer onRequest(YAMLParser parser, ObjectReader objectReader, long s, FluxSink<T> sink) {
        return new LongConsumer() {
            long skip = s;

            @Override
            public void accept(long request) {
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
                            sink.next(item);
                        }
                    }

                    if (token == null || token.isStructEnd()) {
                        parser.close();
                        sink.complete();
                    }
                } catch (Throwable e) {
                    try {
                        parser.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                    sink.error(e);
                }
            }
        };
    }
}