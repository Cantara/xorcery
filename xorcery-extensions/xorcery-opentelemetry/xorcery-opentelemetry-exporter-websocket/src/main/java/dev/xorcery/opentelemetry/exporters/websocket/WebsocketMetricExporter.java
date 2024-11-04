package dev.xorcery.opentelemetry.exporters.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.apache.logging.log4j.spi.ExtendedLogger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class WebsocketMetricExporter
        implements MetricExporter {
    private final AggregationTemporality aggregationTemporality;
    private final ExtendedLogger logger;
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final WebsocketExporterService websocketExporterService;
    private final Map<Resource, JsonNode> resourceJson = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public WebsocketMetricExporter(WebsocketExporterService websocketExporterService, AggregationTemporality aggregationTemporality, ExtendedLogger logger) {
        this.websocketExporterService = websocketExporterService;
        this.aggregationTemporality = aggregationTemporality;
        this.logger = logger;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (this.isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }

        logger.debug("Received a collection of " + metrics.size() + " metrics for export.");
        for (MetricData metric : metrics) {
            if (metric.isEmpty())
                continue;

            Metadata metadata = new Metadata.Builder()
                    .add("type", "metric")
                    .add("resource", resourceJson.computeIfAbsent(metric.getResource(), resource ->
                    {
                        Map<String, Object> resourceObject = resource.getAttributes().asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
                        return objectMapper.valueToTree(resourceObject);
                    }))
                    .build();
            ObjectNode metricJson = JsonNodeFactory.instance.objectNode();
            metricJson.put("name", metric.getName());
            if (metric.getDescription() != null && !metric.getDescription().isEmpty())
                metricJson.put("description", metric.getDescription());
            if (metric.getUnit() != null && !metric.getUnit().isEmpty())
                metricJson.put("unit", metric.getUnit());
            metricJson.set("data", objectMapper.valueToTree(metric.getData()));

            websocketExporterService.send(new MetadataByteBuffer(metadata, ByteBuffer.wrap(metricJson.toString().getBytes(StandardCharsets.UTF_8))));
        }
        return CompletableResultCode.ofSuccess();
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
