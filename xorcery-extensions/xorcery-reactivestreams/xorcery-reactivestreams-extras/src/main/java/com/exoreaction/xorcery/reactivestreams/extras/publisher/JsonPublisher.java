package com.exoreaction.xorcery.reactivestreams.extras.publisher;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.yaml.snakeyaml.LoaderOptions;
import reactor.core.publisher.Flux;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class JsonPublisher<T>
        implements Publisher<T> {

    private static final ObjectReader jsonReader = new JsonMapper().reader();
    private final Flux<T> flux;

    public JsonPublisher(Class<T> itemType, URL yamlResource) {
        flux = Flux.push(sink -> {
            try {
                InputStream resourceAsStream = new BufferedInputStream(yamlResource.openStream(), 32 * 1024);
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
                YAMLFactory factory = YAMLFactory.builder().loaderOptions(loaderOptions).build();
                new ObjectReaderStreamer<>(sink, factory.createParser(resourceAsStream), jsonReader.forType(itemType));
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        flux.subscribe(s);
    }

}
