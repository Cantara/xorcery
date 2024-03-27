package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.extras.publisher.ObjectReaderStreamer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.concurrent.atomic.AtomicInteger;

public final class JsonYamlPublisher
        implements Publisher<WithMetadata<ArrayNode>> {

    private static final ObjectReader yamlReader = new YAMLMapper().reader();
    private final Flux<JsonNode> flux;

    public JsonYamlPublisher(URL yamlResource) {
        flux = Flux.<JsonNode>push(sink -> {
            try {
                InputStream resourceAsStream = new BufferedInputStream(yamlResource.openStream(), 32 * 1024);
                LoaderOptions loaderOptions = new LoaderOptions();
                loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
                YAMLFactory factory = YAMLFactory.builder().loaderOptions(loaderOptions).build();
                new ObjectReaderStreamer<>(sink, factory.createParser(resourceAsStream), yamlReader.forType(JsonNode.class));
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }

    @Override
    public void subscribe(Subscriber<? super WithMetadata<ArrayNode>> s) {
        AtomicInteger revision = new AtomicInteger();
        flux.map(json ->
        {
            if (json.path("metadata") instanceof ObjectNode metadata) {
                metadata.set("revision", metadata.numberNode(revision.getAndIncrement()));
                return new WithMetadata<ArrayNode>(new Metadata(metadata), (ArrayNode) json.path("events"));
            }
            throw new IllegalArgumentException();
        }).subscribe(s);
    }
}
