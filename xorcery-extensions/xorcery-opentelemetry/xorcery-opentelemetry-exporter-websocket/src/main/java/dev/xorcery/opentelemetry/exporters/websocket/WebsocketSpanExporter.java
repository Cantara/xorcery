package dev.xorcery.opentelemetry.exporters.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class WebsocketSpanExporter
    implements SpanExporter
{
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final WebsocketExporterService attachSender;
    private final Logger logger;
    private final Map<Resource, JsonNode> resourceJson = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebsocketSpanExporter(WebsocketExporterService attachSender, Logger logger) {
        this.attachSender = attachSender;
        this.logger = logger;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (this.isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }

        logger.debug("Received a collection of " + spans.size() + " spans for export.");
        for (SpanData span : spans) {
            Metadata metadata = new Metadata.Builder()
                    .add("type", "trace")
                    .add("resource", resourceJson.computeIfAbsent(span.getResource(), resource ->
                    {
                        Map<String, Object> resourceObject = resource.getAttributes().asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
                        return objectMapper.valueToTree(resourceObject);
                    }))
                    .build();
            attachSender.send(new MetadataByteBuffer(metadata, ByteBuffer.wrap(span.toString().getBytes(StandardCharsets.UTF_8))));
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
