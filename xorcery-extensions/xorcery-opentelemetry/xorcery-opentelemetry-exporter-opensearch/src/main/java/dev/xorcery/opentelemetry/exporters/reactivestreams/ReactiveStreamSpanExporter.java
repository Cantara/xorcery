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
import io.opentelemetry.exporter.internal.marshal.Marshaler;
import io.opentelemetry.exporter.internal.otlp.traces.SpanReusableDataMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
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

@Service(name = "opentelemetry.exporters.reactivestreams.traces")
@ContractsProvided(SpanExporter.class)
public class ReactiveStreamSpanExporter
    implements SpanExporter
{
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final ReactiveStreamExporterService reactiveStreamExporterService;
    private final JsonNode resource;
    private final Logger logger;
    private final ByteArrayOutputStream2 baos = new ByteArrayOutputStream2();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SpanReusableDataMarshaler marshaler;

    @Inject
    public ReactiveStreamSpanExporter(ReactiveStreamExporterService reactiveStreamExporterService, Resource resource, Logger logger) {
        this.reactiveStreamExporterService = reactiveStreamExporterService;
        this.logger = logger;
        marshaler = new SpanReusableDataMarshaler(MemoryMode.REUSABLE_DATA, this::doExport);
        Map<String, Object> resourceObject = resource.getAttributes().asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
        this.resource = objectMapper.valueToTree(resourceObject);
    }

    private CompletableResultCode doExport(Marshaler marshaler, Integer integer) {
        try {
            JsonGenerator jsonGenerator = objectMapper.createGenerator(baos);
            marshaler.writeJsonToGenerator(jsonGenerator);
            JsonNode spanJson = objectMapper.readTree(new ByteArrayInputStream(baos.getBuf()));
            Metadata metadata = new Metadata.Builder()
                    .add("type", "trace")
                    .add("resource", resource)
                    .build();

            reactiveStreamExporterService.send(new MetadataJsonNode<>(metadata, spanJson));
            return CompletableResultCode.ofSuccess();
        } catch (IOException e) {
            return CompletableResultCode.ofExceptionalFailure(e);
        }
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (this.isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }

        logger.debug("Received a collection of " + spans.size() + " spans for export.");
        return marshaler.export(spans);
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
