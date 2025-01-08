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
package dev.xorcery.opentelemetry.exporters.reactivestreams;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import io.opentelemetry.exporter.internal.otlp.metrics.MetricsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service(name="opentelemetry.exporters.reactivestreams.metrics")
@ContractsProvided(MetricExporter.class)
public class ReactiveStreamMetricExporter
        implements MetricExporter {
    private final AggregationTemporality aggregationTemporality;
    private final JsonNode resource;
    private final Logger logger;
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final ReactiveStreamExporterService reactiveStreamExporterService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ByteArrayOutputStream2 baos = new ByteArrayOutputStream2();

    @Inject
    public ReactiveStreamMetricExporter(ReactiveStreamExporterService reactiveStreamExporterService, Resource resource, Logger logger) {
        this.reactiveStreamExporterService = reactiveStreamExporterService;
        this.aggregationTemporality = AggregationTemporality.CUMULATIVE;
        this.logger = logger;
        Map<String, Object> resourceObject = resource.getAttributes().asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
        this.resource = objectMapper.valueToTree(resourceObject);
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (this.isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }

        logger.debug("Received a collection of " + metrics.size() + " metrics for export.");

        try {
            JsonGenerator jsonGenerator = objectMapper.createGenerator(baos);
            MetricsRequestMarshaler.create(metrics).writeJsonToGenerator(jsonGenerator);
            JsonNode metricJson = objectMapper.readTree(new ByteArrayInputStream(baos.getBuf()));

            Metadata metadata = new Metadata.Builder()
                    .add("type", "metric")
                    .add("resource", resource)
                    .build();
            reactiveStreamExporterService.send(new MetadataJsonNode<>(metadata, metricJson));
            return CompletableResultCode.ofSuccess();
        } catch (IOException e) {
            return CompletableResultCode.ofExceptionalFailure(e);
        }
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        isShutdown.set(true);
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return aggregationTemporality;
    }
}
