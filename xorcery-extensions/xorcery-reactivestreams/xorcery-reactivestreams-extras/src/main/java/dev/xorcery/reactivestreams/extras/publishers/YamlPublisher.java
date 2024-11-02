package dev.xorcery.reactivestreams.extras.publishers;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.yaml.snakeyaml.LoaderOptions;
import reactor.core.CoreSubscriber;

import java.io.BufferedInputStream;
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
    private final Class<? super T> itemType;

    public YamlPublisher(Class<? super T> itemType) {
        this.itemType = itemType;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {

        if (s instanceof CoreSubscriber<? super T> coreSubscriber)
        {
            try {
                Object resourceUrl = new ContextViewElement(coreSubscriber.currentContext()).get(ResourcePublisherContext.resourceUrl)
                        .orElseThrow(ContextViewElement.missing(ResourcePublisherContext.resourceUrl));
                URL yamlResource = resourceUrl instanceof URL url ? url : new URL(resourceUrl.toString());
                InputStream resourceAsStream = new BufferedInputStream(yamlResource.openStream(), 32 * 1024);
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
                YAMLFactory factory = YAMLFactory.builder().loaderOptions(loaderOptions).build();
                coreSubscriber.onSubscribe(new ObjectReaderStreamer<>(coreSubscriber, factory.createParser(resourceAsStream), yamlReader.forType(itemType)));
            } catch (Throwable e) {
                coreSubscriber.onError(e);
            }
        } else
        {
            s.onError(new IllegalArgumentException("Subscriber must implement CoreSubscriber"));
        }
    }

}
