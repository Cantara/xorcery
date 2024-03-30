package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.metadata.Metadata;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
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

    // Metadata
    public CompletableFuture<WriteResult> setStreamMetadata(String streamName, AppendToStreamOptions options, StreamMetadata metadata) {
        return client.setStreamMetadata(streamName, options, metadata);
    }

    public CompletableFuture<StreamMetadata> getStreamMetadata(String streamName) {
        return client.getStreamMetadata(streamName);
    }

    // Write
    public BiConsumer<AppendMetadataByteBuffers, SynchronousSink<EventStoreCommit>> appender(
            Function<Metadata, UUID> eventIdSelector,
            Function<Metadata, String> eventTypeSelector,
            AppendToStreamOptions options) {
        return new AppendHandler(client, options, eventIdSelector, eventTypeSelector, loggerContext.getLogger(AppendHandler.class), openTelemetry);
    }

    public Function<Context, Context> lastPosition(String streamName) {
        return new LastPositionContext(client, streamName);
    }

    // Read
    public Publisher<MetadataByteBuffer> readStream(String eventstream) {
        return new ReadStream(client, eventstream, loggerContext.getLogger(ReadStreamSubscription.class));
    }

    public Publisher<MetadataByteBuffer> readStreamSubscription(String eventstream) {
        return new ReadStreamSubscription(client, eventstream, loggerContext.getLogger(ReadStreamSubscription.class));
    }
}
