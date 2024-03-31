package com.exoreaction.xorcery.eventstore.client.api;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.eventstore.client.AppendHandler;
import com.exoreaction.xorcery.eventstore.client.LastPositionContext;
import com.exoreaction.xorcery.eventstore.client.ReadStream;
import com.exoreaction.xorcery.eventstore.client.ReadStreamSubscription;
import com.exoreaction.xorcery.metadata.Metadata;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class EventStoreClient {

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
            return new EventStoreClient(settings, loggerContext, openTelemetry);
        }
    }

    private final EventStoreDBClient client;
    private final LoggerContext loggerContext;
    private final OpenTelemetry openTelemetry;

    public EventStoreClient(EventStoreDBClientSettings settings, LoggerContext loggerContext, OpenTelemetry openTelemetry) {
        client = EventStoreDBClient.create(settings);
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
    public BiConsumer<AppendMetadataByteBuffers, SynchronousSink<EventStoreCommit>> appender(
            Function<Metadata, UUID> eventIdSelector,
            Function<Metadata, String> eventTypeSelector,
            Consumer<AppendToStreamOptions> optionsConfigurer) {
        return new AppendHandler(client, optionsConfigurer, eventIdSelector, eventTypeSelector, loggerContext.getLogger(AppendHandler.class), openTelemetry);
    }

    /**
     * Use this with {@link Flux#transformDeferredContextual(BiFunction)}
     *
     * @param streamId id of streamId to get last written streamPosition of. If null then check if Context has "streamId" set
     * @return Flux which writes streamId streamPosition if it exists, or an error if something went wrong
     */
    public <T> BiFunction<Flux<T>, ContextView, Publisher<T>> lastPosition(String streamId) {
        return new LastPositionContext<>(client, streamId);
    }

    // Read
    public Publisher<MetadataByteBuffer> readStream(String streamId) {
        return new ReadStream(client, streamId, loggerContext.getLogger(ReadStream.class));
    }

    public Publisher<MetadataByteBuffer> readStreamSubscription(String streamId) {
        return new ReadStreamSubscription(client, streamId, loggerContext.getLogger(ReadStreamSubscription.class));
    }
}
