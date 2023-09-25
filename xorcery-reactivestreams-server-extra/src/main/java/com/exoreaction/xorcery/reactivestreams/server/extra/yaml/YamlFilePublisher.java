package com.exoreaction.xorcery.reactivestreams.server.extra.yaml;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import org.apache.logging.log4j.LogManager;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class YamlFilePublisher
        implements Publisher<WithMetadata<ArrayNode>> {
    private final URL yamlResource;

    public YamlFilePublisher(URL yamlResource) {
        this.yamlResource = yamlResource;
    }

    @Override
    public void subscribe(Subscriber<? super WithMetadata<ArrayNode>> subscriber) {
        try {
            new YamlEventPublisher(yamlResource, subscriber);
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

    public static class YamlEventPublisher
            implements Subscription {
        private final YAMLParser parser;
        private final InputStream resourceAsStream;
        private final Subscriber<? super WithMetadata<ArrayNode>> subscriber;
        private final YAMLMapper yamlMapper;

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicLong requests = new AtomicLong();
        private long revision = 0;

        public YamlEventPublisher(URL yamlResource, Subscriber<? super WithMetadata<ArrayNode>> subscriber)
                throws IOException {
            this.subscriber = subscriber;
            yamlMapper = new YAMLMapper();
            this.resourceAsStream = yamlResource.openStream();
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setCodePointLimit(100 * 1024 * 1024);
            YAMLFactory factory = YAMLFactory.builder().loaderOptions(loaderOptions).build();
            parser = factory.createParser(resourceAsStream);
            subscriber.onSubscribe(this);
        }

        @Override
        public void request(final long count) {

            if (!cancelled.get() && requests.addAndGet(count) == count) {
                // No drainer running, start one
                CompletableFuture.runAsync(this::drain);
            }
            LogManager.getLogger().trace("Reqest "+count+" "+cancelled.get()+" "+requests.get());
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                subscriber.onComplete();
            }
        }

        public void drain() {
            LogManager.getLogger().trace("Drain begin");
            try {
                JsonToken token = null;
                while (!cancelled.get() && requests.getAndDecrement() > 0 && !(token = parser.nextToken()).isStructEnd()) {
                    //                    System.out.println(token);
                    parser.nextToken();
                    ObjectNode node = yamlMapper.readTree(parser);
                    ObjectNode metadata = (ObjectNode)node.get("metadata");
                    JsonNode event = node.get("events");
                    metadata.set("revision", JsonNodeFactory.instance.numberNode(revision));
                    WithMetadata<ArrayNode> withMetadata = new WithMetadata<>(new Metadata((ObjectNode) metadata), (ArrayNode) event);
                    subscriber.onNext(withMetadata);
                    revision++;
                }

                // Compensate the final decrement
                requests.incrementAndGet();

                if (token == null || token.isStructEnd()) {
                    if (cancelled.compareAndSet(false, true)) {
                        resourceAsStream.close();
                        subscriber.onComplete();
                        LogManager.getLogger().trace("Import complete");
                    }
                }
            } catch (Throwable e) {
                if (cancelled.compareAndSet(false, true)) {
                    subscriber.onError(e);
                }
                try {
                    resourceAsStream.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }

            if (!cancelled.get() && requests.get() > 0)
                drain();
            else
            {
                LogManager.getLogger().trace("Drain complete");
            }
        }
    }
}
