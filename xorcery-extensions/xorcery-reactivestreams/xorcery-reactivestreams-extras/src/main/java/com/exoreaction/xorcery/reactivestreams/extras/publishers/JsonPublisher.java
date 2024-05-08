package com.exoreaction.xorcery.reactivestreams.extras.publishers;

import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.yaml.snakeyaml.LoaderOptions;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

public class JsonPublisher<T>
        implements Publisher<T> {

    private static final ObjectReader jsonReader = new JsonMapper().reader();
    private final Class<? super T> itemType;

    public JsonPublisher(Class<? super T> itemType) {
        this.itemType = itemType;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        if (s instanceof CoreSubscriber<? super T> coreSubscriber)
        {
            try {
                Object resourceUrl = new ContextViewElement(coreSubscriber.currentContext()).get(ResourcePublisherContext.resourceUrl)
                        .orElseThrow(ContextViewElement.missing(ResourcePublisherContext.resourceUrl));
                URL jsonResource = resourceUrl instanceof URL url ? url : new URL(resourceUrl.toString());
                InputStream resourceAsStream = new BufferedInputStream(jsonResource.openStream(), 32 * 1024);
                JsonFactory factory = JsonFactory.builder().build();
                coreSubscriber.onSubscribe(new ObjectReaderStreamer<>(coreSubscriber, factory.createParser(resourceAsStream), jsonReader.forType(itemType)));
            } catch (Throwable e) {
                coreSubscriber.onError(e);
            }
        } else
        {
            s.onError(new IllegalArgumentException("Subscriber must implement CoreSubscriber"));
        }
    }
}
