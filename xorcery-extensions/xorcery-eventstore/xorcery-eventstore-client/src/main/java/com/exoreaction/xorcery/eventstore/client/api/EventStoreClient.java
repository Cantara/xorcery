package com.exoreaction.xorcery.eventstore.client.api;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.eventstore.client.AppendHandler;
import com.exoreaction.xorcery.eventstore.client.AppendOptimisticLockingHandler;
import com.exoreaction.xorcery.eventstore.client.ReadStreamSubscription2;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.disruptor.DisruptorConfiguration;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.ContextView;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class EventStoreClient
    implements Closeable
{

    @Service
    public static class Factory {
        private final OpenTelemetry openTelemetry;
        private final LoggerContext loggerContext;

        @Inject
        public Factory(OpenTelemetry openTelemetry, LoggerContext loggerContext) {
            this.openTelemetry = openTelemetry;
            this.loggerContext = loggerContext;
        }

        public EventStoreClient create(EventStoreDBClientSettings settings) {
            return create(settings, new DisruptorConfiguration(new Configuration.Builder().build()));
        }

        public EventStoreClient create(EventStoreDBClientSettings settings, DisruptorConfiguration disruptorConfiguration) {
            return new EventStoreClient(settings, disruptorConfiguration, loggerContext, openTelemetry);
        }
    }

    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final DisruptorConfiguration disruptorConfiguration;
    private final LoggerContext loggerContext;
    private final OpenTelemetry openTelemetry;

    public EventStoreClient(EventStoreDBClientSettings settings, DisruptorConfiguration disruptorConfiguration, LoggerContext loggerContext, OpenTelemetry openTelemetry) {
        client = EventStoreDBClient.create(settings);
        this.disruptorConfiguration = disruptorConfiguration;
        this.loggerContext = loggerContext;
        this.openTelemetry = openTelemetry;
    }

    public EventStoreDBClient getClient() {
        return client;
    }

    // Metadata
    public CompletableFuture<WriteResult> setStreamMetadata(String streamId, AppendToStreamOptions options, StreamMetadata metadata) {
        return client.setStreamMetadata(streamId, options, metadata);
    }

    public CompletableFuture<StreamMetadata> getStreamMetadata(String streamId) {
        return client.getStreamMetadata(streamId);
    }

    // Write
    public BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> appendStream(
            Function<MetadataByteBuffer, UUID> eventIdSelector,
            Function<MetadataByteBuffer, String> eventTypeSelector,
            Consumer<AppendToStreamOptions> optionsConfigurer) {
        return new AppendHandler(client, optionsConfigurer, disruptorConfiguration, eventIdSelector, eventTypeSelector, loggerContext.getLogger(AppendHandler.class), openTelemetry);
    }

    public BiConsumer<MetadataByteBuffer, SynchronousSink<MetadataByteBuffer>> appendOptimisticLocking(
            Function<MetadataByteBuffer, UUID> eventIdSelector,
            Function<MetadataByteBuffer, String> eventTypeSelector,
            Consumer<AppendToStreamOptions> optionsConfigurer) {
        return new AppendOptimisticLockingHandler(client, optionsConfigurer, eventIdSelector, eventTypeSelector, loggerContext.getLogger(AppendHandler.class), openTelemetry);
    }

    // Read
    public Publisher<MetadataByteBuffer> readStream() {
        return Flux.create(sink -> new ReadStreamSubscription2(client, sink, loggerContext.getLogger(ReadStreamSubscription2.class)));
    }

    @Override
    public void close() throws IOException {
        client.shutdown();
    }
}
