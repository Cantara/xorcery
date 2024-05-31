package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreMetadata;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryHelpers;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.api.ReactiveStreamsContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.SynchronousSink;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;
import static com.exoreaction.xorcery.reactivestreams.api.ContextViewElement.missing;

public class AppendOptimisticLockingHandler
    extends BaseAppendHandler
        implements BiConsumer<MetadataByteBuffer, SynchronousSink<MetadataByteBuffer>> {

    public AppendOptimisticLockingHandler(
            EventStoreDBClient client,
            Consumer<AppendToStreamOptions> options,
            Function<MetadataByteBuffer, UUID> eventIdSelector,
            Function<MetadataByteBuffer, String> eventTypeSelector,
            Logger logger,
            OpenTelemetry openTelemetry) {
        super(client, options, eventIdSelector, eventTypeSelector, logger, openTelemetry);
    }

    @Override
    public void accept(MetadataByteBuffer metadataByteBuffer, SynchronousSink<MetadataByteBuffer> sink) {
        ContextViewElement contextElement = new ContextViewElement(sink.contextView());
        try {
            String streamId = contextElement.getString(ReactiveStreamsContext.streamId).orElseThrow(missing(ReactiveStreamsContext.streamId));
            Attributes attributes = Attributes.of(AttributeKey.stringKey("eventstore.streamId"), streamId);

            OpenTelemetryHelpers.time(writeTimer, attributes, () -> {

                // Prepare the data
                EventData eventData;
                try {
                    UUID eventId = eventIdSelector.apply(metadataByteBuffer);
                    String eventType = eventTypeSelector.apply(metadataByteBuffer);

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
