package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryHelpers;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ReactiveStreamsContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.SynchronousSink;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class AppendOptimisticLockingHandler
        implements BiConsumer<MetadataByteBuffer, SynchronousSink<MetadataByteBuffer>> {
    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final Consumer<AppendToStreamOptions> options;

    private final Function<Metadata, UUID> eventIdSelector;
    private final Function<Metadata, String> eventTypeSelector;

    private final Logger logger;

    // Metrics
    private final DoubleHistogram writeTimer;

    public AppendOptimisticLockingHandler(
            EventStoreDBClient client,
            Consumer<AppendToStreamOptions> options,
            Function<Metadata, UUID> eventIdSelector,
            Function<Metadata, String> eventTypeSelector,
            Logger logger,
            OpenTelemetry openTelemetry) {
        this.client = client;
        this.options = options != null ? options : o -> {
        };
        this.logger = logger;

        // Metrics
        this.eventIdSelector = eventIdSelector;
        this.eventTypeSelector = eventTypeSelector;

        // Metrics
        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        writeTimer = meter.histogramBuilder("eventstore.streamId.writes.latency")
                .setUnit("s").build();

    }

    @Override
    public void accept(MetadataByteBuffer metadataByteBuffer, SynchronousSink<MetadataByteBuffer> sink) {

        String streamId = ReactiveStreamsContext.getContext(sink.contextView(), ReactiveStreamsContext.streamId);
        Attributes attributes = Attributes.of(AttributeKey.stringKey("eventstore.streamId"), streamId);
        try {
            OpenTelemetryHelpers.time(writeTimer, attributes, () -> {

                // Prepare the data
                EventData eventData;
                try {
                    UUID eventId = eventIdSelector.apply(metadataByteBuffer.metadata());
                    String eventType = eventTypeSelector.apply(metadataByteBuffer.metadata());

                    eventData = EventDataBuilder.json(eventId, eventType, metadataByteBuffer.data().array())
                            .metadataAsBytes(jsonMapper.writeValueAsBytes(metadataByteBuffer.metadata().json())).build();
                } catch (JsonProcessingException e) {
                    sink.error(e);
                    return null;
                }

                // Append to EventStore, and retry on timeouts
                AppendToStreamOptions appendOptions = AppendToStreamOptions.get()
                        .deadline(30000);
                options.accept(appendOptions);
                metadataByteBuffer.metadata().getLong(EventStoreMetadata.expectedPosition).ifPresent(appendOptions::expectedRevision);

                int retry = 0;
                while (true) {
                    try {

                        WriteResult writeResult = client.appendToStream(streamId, appendOptions, eventData)
                                .toCompletableFuture().join();

                        long streamPosition = writeResult.getNextExpectedRevision().toRawLong();
                        metadataByteBuffer.metadata().toBuilder().add(EventStoreMetadata.streamPosition, streamPosition);
                        sink.next(metadataByteBuffer);
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
