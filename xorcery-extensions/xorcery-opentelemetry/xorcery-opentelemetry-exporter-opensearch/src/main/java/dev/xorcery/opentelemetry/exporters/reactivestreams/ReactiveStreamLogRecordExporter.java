package dev.xorcery.opentelemetry.exporters.reactivestreams;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.eclipse.jetty.util.ByteArrayOutputStream2;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ReactiveStreamLogRecordExporter
        implements LogRecordExporter {
    private final ReactiveStreamExporterService reactiveStreamExporterService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final ByteArrayOutputStream2 baos = new ByteArrayOutputStream2();
    private final JsonNode resource;

    public ReactiveStreamLogRecordExporter(ReactiveStreamExporterService reactiveStreamExporterService, Resource resource, ObjectMapper objectMapper) {
        this.reactiveStreamExporterService = reactiveStreamExporterService;
        this.objectMapper = objectMapper;
        Map<String, Object> resourceObject = resource.getAttributes().asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
        this.resource = objectMapper.valueToTree(resourceObject);
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        if (this.isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }
        try {
            JsonGenerator jsonGenerator = objectMapper.createGenerator(baos);
            LogsRequestMarshaler.create(logs).writeJsonToGenerator(jsonGenerator);
            JsonNode logJson = objectMapper.readTree(new ByteArrayInputStream(baos.getBuf()));
            Metadata metadata = new Metadata.Builder()
                    .add("type", "log")
                    .add("resource", resource)
                    .build();

            reactiveStreamExporterService.send(new MetadataJsonNode<>(metadata, logJson));
            return CompletableResultCode.ofSuccess();
        } catch (Throwable e) {
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
}
