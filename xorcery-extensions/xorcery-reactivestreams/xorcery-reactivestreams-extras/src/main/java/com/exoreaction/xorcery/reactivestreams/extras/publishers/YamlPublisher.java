package com.exoreaction.xorcery.reactivestreams.extras.publishers;

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

/**
 * Put the URL to the resource to be published into the subscriber ContextView with key {@link ResourcePublisherContext#resourceUrl}
 *
 * @param <T>
 */
public class YamlPublisher<T>
        implements Publisher<T> {

    private static final ObjectReader yamlReader = new YAMLMapper().reader();
    private final Flux<T> flux;

    public YamlPublisher(Class<? super T> itemType) {
        flux = Flux.push(sink -> {
            try {
                Object resourceUrl = sink.contextView().get(ResourcePublisherContext.resourceUrl.name());
                URL yamlResource = resourceUrl instanceof URL url ? url : new URL(resourceUrl.toString());
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
