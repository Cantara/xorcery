package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryHelpers;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ReactiveStreamsContext;
import com.exoreaction.xorcery.reactivestreams.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.reactivestreams.disruptor.SmartBatching;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class AppendHandler
    extends BaseAppendHandler
        implements BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> {

    private final SmartBatching<MetadataByteBuffer> smartBatching;


    public AppendHandler(
            EventStoreDBClient client,
            Consumer<AppendToStreamOptions> options,
            DisruptorConfiguration configuration,
            Function<Metadata, UUID> eventIdSelector,
            Function<Metadata, String> eventTypeSelector,
            Logger logger,
            OpenTelemetry openTelemetry) {
        super(client, options, eventIdSelector, eventTypeSelector, logger, openTelemetry);
        this.smartBatching = new SmartBatching<>(configuration, this::handler);
    }

    @Override
    public Publisher<MetadataByteBuffer> apply(Flux<MetadataByteBuffer> metadataByteBufferFlux, ContextView contextView) {
        return smartBatching.apply(metadataByteBufferFlux.contextWrite(setStreamMetadata()).contextWrite(addLastPosition()), contextView);
    }

    private void handler(List<MetadataByteBuffer> metadataByteBuffers, SynchronousSink<List<MetadataByteBuffer>> sink) {
        String streamId = ReactiveStreamsContext.getContext(sink.contextView(), ReactiveStreamsContext.streamId);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("eventstore.streamId"), streamId);
        try {
            OpenTelemetryHelpers.time(writeTimer, attributes, () -> {

                // Prepare the data
                List<EventData> eventBatch = new ArrayList<>();
                try {
                    for (MetadataByteBuffer metadataByteBuffer : metadataByteBuffers) {
                        UUID eventId = eventIdSelector.apply(metadataByteBuffer.metadata());
                        String eventType = eventTypeSelector.apply(metadataByteBuffer.metadata());

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
