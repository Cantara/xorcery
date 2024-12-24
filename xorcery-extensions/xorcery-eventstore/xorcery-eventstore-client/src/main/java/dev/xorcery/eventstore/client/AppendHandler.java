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
package dev.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import dev.xorcery.eventstore.client.api.EventStoreMetadata;
import dev.xorcery.opentelemetry.OpenTelemetryHelpers;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import dev.xorcery.reactivestreams.extras.operators.SmartBatchingOperator;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.xorcery.collections.Element.missing;
import static dev.xorcery.lang.Exceptions.unwrap;
import static io.opentelemetry.context.Context.taskWrapping;

public class AppendHandler
    extends BaseAppendHandler
        implements BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> {

    private final BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> smartBatching;


    public AppendHandler(
            EventStoreDBClient client,
            Consumer<AppendToStreamOptions> options,
            Function<MetadataByteBuffer, UUID> eventIdSelector,
            Function<MetadataByteBuffer, String> eventTypeSelector,
            Logger logger,
            OpenTelemetry openTelemetry) {
        super(client, options, eventIdSelector, eventTypeSelector, logger, openTelemetry);
        this.smartBatching = SmartBatchingOperator.smartBatching(this::handler, ()-> new ArrayBlockingQueue<>(1024), ()-> taskWrapping(Schedulers.boundedElastic()::schedule));
    }

    @Override
    public Publisher<MetadataByteBuffer> apply(Flux<MetadataByteBuffer> metadataByteBufferFlux, ContextView contextView) {
        return smartBatching.apply(metadataByteBufferFlux.contextWrite(setStreamMetadata()).contextWrite(addLastPosition()), contextView);
    }

    private void handler(Collection<MetadataByteBuffer> metadataByteBuffers, SynchronousSink<Collection<MetadataByteBuffer>> sink) {
        ContextViewElement contextElement = new ContextViewElement(sink.contextView());
        try {
            String streamId = contextElement.getString(ReactiveStreamsContext.streamId).orElseThrow(missing(ReactiveStreamsContext.streamId));
            Attributes attributes = Attributes.of(AttributeKey.stringKey("eventstore.streamId"), streamId);
            OpenTelemetryHelpers.time(writeTimer, attributes, () -> {

                // Prepare the data
                List<EventData> eventBatch = new ArrayList<>();
                try {
                    for (MetadataByteBuffer metadataByteBuffer : metadataByteBuffers) {
                        UUID eventId = eventIdSelector.apply(metadataByteBuffer);
                        String eventType = eventTypeSelector.apply(metadataByteBuffer);

                        EventData eventData = EventDataBuilder.json(eventId, eventType, metadataByteBuffer.data().array())
                                .metadataAsBytes(jsonMapper.writeValueAsBytes(metadataByteBuffer.metadata().json())).build();
                        eventBatch.add(eventData);

                    }
                } catch (JsonProcessingException e) {
                    sink.error(e);
                    return null;
                }

                // Append to EventStore, and retry on timeouts
                AppendToStreamOptions appendOptions = AppendToStreamOptions.get()
                        .deadline(30000);
                options.accept(appendOptions);

                int retry = 0;
                while (true) {
                    try {

                        WriteResult writeResult = client.appendToStream(streamId, appendOptions, eventBatch.iterator())
                                .toCompletableFuture().join();

                        long streamPosition = writeResult.getNextExpectedRevision().toRawLong();
                        long eventPosition = streamPosition - metadataByteBuffers.size();
                        for (MetadataByteBuffer metadataByteBuffer : metadataByteBuffers) {
                            metadataByteBuffer.metadata().toBuilder().add(EventStoreMetadata.streamPosition, ++eventPosition);
                        }
                        sink.next(metadataByteBuffers);
                        batchSizes.record(metadataByteBuffers.size());
                        return null;
                    } catch (Throwable t) {
                        Throwable throwable = unwrap(t);
                        if (throwable instanceof StatusRuntimeException sre) {
                            String description = sre.getStatus().getDescription();
                            if (description != null && description.contains("timeout") && ++retry < 10) {
                                // Try again
                                logger.warn("Append timeout, retrying", throwable);
                                continue;
                            }
                        } else if (throwable instanceof NotLeaderException) {
                            // Try again
                            logger.warn("Not leader, retrying", throwable);
                            continue;
                        }
                        sink.error(throwable);
                        return null;
                    }
                }
            });
        } catch (Exception e) {
            sink.error(e);
        }
    }
}
