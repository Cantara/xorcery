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
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataJsonNode;
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import org.eclipse.jetty.util.ByteArrayOutputStream2;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service(name="opentelemetry.exporters.reactivestreams.logs")
@ContractsProvided(LogRecordExporter.class)
public class ReactiveStreamLogRecordExporter
        implements LogRecordExporter {
    private final ReactiveStreamExporterService reactiveStreamExporterService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean isShutdown = new AtomicBoolean();
    private final ByteArrayOutputStream2 baos = new ByteArrayOutputStream2();
    private final JsonNode resource;

    @Inject
    public ReactiveStreamLogRecordExporter(ReactiveStreamExporterService reactiveStreamExporterService, Resource resource) {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LogRecordData.class, new LogRecordDataSerializer());
        objectMapper.registerModule(module);

        this.reactiveStreamExporterService = reactiveStreamExporterService;
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
