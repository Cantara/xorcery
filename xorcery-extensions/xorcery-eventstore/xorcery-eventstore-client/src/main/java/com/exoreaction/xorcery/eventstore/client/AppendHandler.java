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
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class AppendHandler
        implements BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> {
    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final Consumer<AppendToStreamOptions> options;

    private final Function<Metadata, UUID> eventIdSelector;
    private final Function<Metadata, String> eventTypeSelector;

    private final Logger logger;

    // Metrics
    private final LongHistogram batchSizes;
    private final DoubleHistogram writeTimer;
    private final SmartBatching<MetadataByteBuffer> smartBatching;

    public AppendHandler(
            EventStoreDBClient client,
            Consumer<AppendToStreamOptions> options,
            DisruptorConfiguration configuration,
            Function<Metadata, UUID> eventIdSelector,
            Function<Metadata, String> eventTypeSelector,
            Logger logger,
            OpenTelemetry openTelemetry) {
        this.client = client;
        this.options = options != null ? options : o -> {
        };
        this.logger = logger;
        this.smartBatching = new SmartBatching<>(configuration, this::handler);
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
    public Publisher<MetadataByteBuffer> apply(Flux<MetadataByteBuffer> metadataByteBufferFlux, ContextView contextView) {
        return smartBatching.apply(metadataByteBufferFlux.contextWrite(addLastPosition(contextView)), contextView);
    }

    private Function<Context, Context> addLastPosition(ContextView contextView) {
        return context ->
        {
            try {
                String streamId = ReactiveStreamsContext.getContext(contextView, ReactiveStreamsContext.streamId);
                return client.readStream(streamId, ReadStreamOptions.get().backwards().maxCount(1))
                        .orTimeout(10, TimeUnit.SECONDS).thenApply(readResult ->
                        {
                            long lastStreamPosition = readResult.getLastStreamPosition();
                            return context.put(ReactiveStreamsContext.streamPosition, lastStreamPosition);
                        }).exceptionallyCompose(throwable ->
                        {
                            if (throwable.getCause() instanceof StreamNotFoundException) {
                                return CompletableFuture.completedStage(context);
                            } else {
                                return CompletableFuture.failedStage(throwable.getCause());
                            }
                        }).orTimeout(10, TimeUnit.SECONDS).join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
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
                        long eventPosition = streamPosition-metadataByteBuffers.size();
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
