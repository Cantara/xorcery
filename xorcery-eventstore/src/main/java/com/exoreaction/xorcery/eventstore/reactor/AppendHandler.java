package com.exoreaction.xorcery.eventstore.reactor;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryHelpers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.SynchronousSink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class AppendHandler
        implements BiConsumer<AppendMetadataByteBuffers, SynchronousSink<EventStoreCommit>> {
    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final AppendToStreamOptions options;

    private final Function<Metadata, UUID> eventIdSelector;
    private final Function<Metadata, String> eventTypeSelector;

    private final Logger logger;

    // Metrics
    private final LongHistogram batchSizes;
    private final DoubleHistogram writeTimer;

    public AppendHandler(
            EventStoreDBClient client,
            AppendToStreamOptions options,
            Function<Metadata, UUID> eventIdSelector,
            Function<Metadata, String> eventTypeSelector,
            Logger logger,
            OpenTelemetry openTelemetry) {
        this.client = client;
        this.options = options;
        this.logger = logger;
        // Metrics
        this.eventIdSelector = eventIdSelector;
        this.eventTypeSelector = eventTypeSelector;

        // Metrics
        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        batchSizes = meter.histogramBuilder("eventstore.stream.writes.batchsize")
                .ofLongs().setUnit("{count}").build();
        writeTimer = meter.histogramBuilder("eventstore.stream.writes.latency")
                .setUnit("s").build();

    }

    @Override
    public void accept(AppendMetadataByteBuffers appendMetadataByteBuffers, SynchronousSink<EventStoreCommit> sink) {

        try {
            Attributes attributes = Attributes.of(AttributeKey.stringKey("eventstore.stream.name"), appendMetadataByteBuffers.stream());
            OpenTelemetryHelpers.time(writeTimer, attributes, () -> {

                // Prepare the data
                List<EventData> eventBatch = new ArrayList<>();
                UUID lastEventId = null;
                try {
                    for (MetadataByteBuffer metadataByteBuffer : appendMetadataByteBuffers.items()) {
                        UUID eventId = eventIdSelector.apply(metadataByteBuffer.metadata());
                        lastEventId = eventId;
                        String eventType = eventTypeSelector.apply(metadataByteBuffer.metadata());

                        EventData eventData = EventDataBuilder.binary(eventId, eventType, metadataByteBuffer.event().array())
                                .metadataAsBytes(jsonMapper.writeValueAsBytes(metadataByteBuffer.metadata().json())).build();
                        eventBatch.add(eventData);

                    }
                } catch (JsonProcessingException e) {
                    sink.error(e);
                    return null;
                }

                // Append to EventStore, and retry on timeouts
                int retry = 0;
                while (true) {
                    try {
                        WriteResult writeResult = client.appendToStream(appendMetadataByteBuffers.stream(), options, eventBatch.iterator())
                                .toCompletableFuture().join();

                        long position = writeResult.getNextExpectedRevision().toRawLong();
                        sink.next(new EventStoreCommit(appendMetadataByteBuffers.stream(), lastEventId != null ? lastEventId.toString() : null, position));
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
