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
package dev.xorcery.kurrent.client.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import io.kurrent.dbclient.*;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SchemaUrls;
import org.apache.logging.log4j.Logger;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.xorcery.collections.Element.missing;


class BaseAppendHandler {

    static final ObjectMapper jsonMapper = new JsonMapper().findAndRegisterModules();

    final KurrentDBClient client;
    final Consumer<AppendToStreamOptions> options;

    final Function<MetadataByteBuffer, UUID> eventIdSelector;
    final Function<MetadataByteBuffer, String> eventTypeSelector;

    final Logger logger;

    // Metrics
    final LongHistogram batchSizes;
    final DoubleHistogram writeTimer;

    BaseAppendHandler(
            KurrentDBClient client,
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
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        batchSizes = meter.histogramBuilder("kurrent.stream.writes.batchsize")
                .ofLongs().setUnit("{count}").build();
        writeTimer = meter.histogramBuilder("kurrent.stream.writes.latency")
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
                        case "request", "response" -> {}
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
