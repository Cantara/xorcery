package com.exoreaction.xorcery.reactivestreams.extras.publisher;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.yaml.snakeyaml.LoaderOptions;
import reactor.core.publisher.Flux;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class YamlPublisher<T>
        implements Publisher<T> {

    private static final ObjectReader yamlReader = new YAMLMapper().reader();
    private final Flux<T> flux;

    public YamlPublisher(Class<T> itemType, URL yamlResource) {
        flux = Flux.<T>push(sink -> {
            try {
                InputStream resourceAsStream = new BufferedInputStream(yamlResource.openStream(), 32 * 1024);
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
                YAMLFactory factory = YAMLFactory.builder().loaderOptions(loaderOptions).build();
                new ObjectReaderStreamer<>(sink, factory.createParser(resourceAsStream), yamlReader.forType(itemType));
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
