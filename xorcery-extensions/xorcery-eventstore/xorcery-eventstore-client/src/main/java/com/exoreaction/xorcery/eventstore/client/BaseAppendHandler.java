package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ReactiveStreamsContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.exoreaction.xorcery.reactivestreams.api.reactor.ContextViewElement.missing;

class BaseAppendHandler {

    static final JsonMapper jsonMapper = new JsonMapper();

    final EventStoreDBClient client;
    final Consumer<AppendToStreamOptions> options;

    final Function<MetadataByteBuffer, UUID> eventIdSelector;
    final Function<MetadataByteBuffer, String> eventTypeSelector;

    final Logger logger;

    // Metrics
    final LongHistogram batchSizes;
    final DoubleHistogram writeTimer;

    BaseAppendHandler(
            EventStoreDBClient client,
            Consumer<AppendToStreamOptions> options,
            Function<MetadataByteBuffer, UUID> eventIdSelector,
            Function<MetadataByteBuffer, String> eventTypeSelector,
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
        batchSizes = meter.histogramBuilder("eventstore.stream.writes.batchsize")
                .ofLongs().setUnit("{count}").build();
        writeTimer = meter.histogramBuilder("eventstore.stream.writes.latency")
                .setUnit("s").build();

    }

    protected Function<Context, Context> addLastPosition() {
        return context ->
        {
            try {
                String streamId = new ContextViewElement(context).getString(ReactiveStreamsContext.streamId)
                        .orElseThrow(missing(ReactiveStreamsContext.streamId));
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

    protected Function<Context, Context> setStreamMetadata() {
        return context ->
        {
            if (context.isEmpty())
                return context;

            try {
                String streamId = new ContextViewElement(context).getString(ReactiveStreamsContext.streamId)
                        .orElseThrow(missing(ReactiveStreamsContext.streamId));
                AppendToStreamOptions setMetaDataOptions = AppendToStreamOptions.get();
                options.accept(setMetaDataOptions);
                StreamMetadata streamMetadata = client.getStreamMetadata(streamId)
                        .exceptionallyCompose(exception ->
                        {
                            if (exception.getCause() instanceof StreamNotFoundException) {
                                return CompletableFuture.completedStage(new StreamMetadata());
                            } else {
                                return CompletableFuture.failedStage(exception.getCause());
                            }
                        })
                        .orTimeout(10, TimeUnit.SECONDS).join();
                Map<String, Object> customProperties = new HashMap<>();
                for (Map.Entry<Object, Object> entry : context.stream().toList()) {
                    switch (entry.getKey().toString()) {
                        case "maxAge" ->
                                streamMetadata.setMaxAge(entry.getValue() instanceof Long nr ? nr : Long.valueOf(entry.getValue().toString()));
                        case "maxCount" ->
                                streamMetadata.setMaxCount(entry.getValue() instanceof Long nr ? nr : Long.valueOf(entry.getValue().toString()));
                        case "cacheControl" ->
                                streamMetadata.setCacheControl(entry.getValue() instanceof Long nr ? nr : Long.valueOf(entry.getValue().toString()));
                        case "truncateBefore" ->
                                streamMetadata.setTruncateBefore(entry.getValue() instanceof Long nr ? nr : Long.valueOf(entry.getValue().toString()));
                        case "acl" -> streamMetadata.setAcl(parseAcl(entry.getValue()));
                        case "customProperties" ->
                                customProperties.putAll(jsonMapper.readValue(entry.getValue().toString(), Map.class));
                        default -> customProperties.put(entry.getKey().toString(), entry.getValue());
                    }
                }
                if (!customProperties.isEmpty()) {
                    streamMetadata.setCustomProperties(customProperties);
                }
                client.setStreamMetadata(streamId, setMetaDataOptions, streamMetadata)
                        .orTimeout(10, TimeUnit.SECONDS).join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return context;
        };
    }

    private Acl parseAcl(Object value) throws JsonProcessingException {
        return jsonMapper.readValue(value.toString(), StreamAcl.class);
    }
}
