package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public final class JsonYamlPublisher
    implements Publisher<WithMetadata<ArrayNode>>
{
    private final URL yamlResource;

    public JsonYamlPublisher(URL yamlResource) {
        this.yamlResource = yamlResource;
    }

    @Override
    public void subscribe(Subscriber<? super WithMetadata<ArrayNode>> s) {
        AtomicInteger revision = new AtomicInteger();
        Flux.from(new YamlPublisher<ObjectNode>(ObjectNode.class))
                .map(json -> {
                    if (json.path("metadata") instanceof ObjectNode metadata) {
                        metadata.set("revision", metadata.numberNode(revision.getAndIncrement()));
                        return new WithMetadata<ArrayNode>(new Metadata(metadata), (ArrayNode) json.path("events"));
                    }
                    throw new IllegalArgumentException();
                }).contextWrite(Context.of(ResourcePublisherContext.resourceUrl.name(), yamlResource))
                .subscribe(s);
    }
}
