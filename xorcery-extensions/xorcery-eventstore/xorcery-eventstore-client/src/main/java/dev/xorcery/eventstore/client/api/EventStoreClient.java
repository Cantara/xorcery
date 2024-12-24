/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.eventstore.client.api;

import com.eventstore.dbclient.*;
import dev.xorcery.eventstore.client.AppendHandler;
import dev.xorcery.eventstore.client.AppendOptimisticLockingHandler;
import dev.xorcery.eventstore.client.ReadStreamSubscription;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
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
    public BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> appendStream(
            Function<MetadataByteBuffer, UUID> eventIdSelector,
            Function<MetadataByteBuffer, String> eventTypeSelector,
            Consumer<AppendToStreamOptions> optionsConfigurer) {
        return new AppendHandler(client, optionsConfigurer, eventIdSelector, eventTypeSelector, loggerContext.getLogger(AppendHandler.class), openTelemetry);
    }

    public BiConsumer<MetadataByteBuffer, SynchronousSink<MetadataByteBuffer>> appendOptimisticLocking(
            Function<MetadataByteBuffer, UUID> eventIdSelector,
            Function<MetadataByteBuffer, String> eventTypeSelector,
            Consumer<AppendToStreamOptions> optionsConfigurer) {
        return new AppendOptimisticLockingHandler(client, optionsConfigurer, eventIdSelector, eventTypeSelector, loggerContext.getLogger(AppendHandler.class), openTelemetry);
    }

    // Read
    public Publisher<MetadataByteBuffer> readStream() {
        return Flux.create(sink -> new ReadStreamSubscription(client, sink, loggerContext.getLogger(ReadStreamSubscription.class)));
    }

    @Override
    public void close() throws IOException {
        client.shutdown();
    }
}
