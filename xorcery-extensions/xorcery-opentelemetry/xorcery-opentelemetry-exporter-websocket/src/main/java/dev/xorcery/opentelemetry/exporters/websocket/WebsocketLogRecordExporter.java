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
package dev.xorcery.opentelemetry.exporters.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service(name="opentelemetry.exporters.websocket.logs")
@ContractsProvided(LogRecordExporter.class)
public class WebsocketLogRecordExporter
        implements LogRecordExporter {
    private final WebsocketExporterService attachSender;
    private final ByteArrayOutputStream baos;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean isShutdown = new AtomicBoolean();

    private final Map<Resource, JsonNode> resourceJson = new HashMap<>();

    @Inject
    public WebsocketLogRecordExporter(WebsocketExporterService attachSender) {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LogRecordData.class, new LogRecordDataSerializer());
        objectMapper.registerModule(module);
        this.attachSender = attachSender;
        baos = new ByteArrayOutputStream();
    }

    @Override
    public CompletableResultCode export(Collection<LogRecordData> logs) {
        if (this.isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        }
        try {
            for (LogRecordData log : logs) {
                Metadata metadata = new Metadata.Builder()
                        .add("type", "log")
                        .add("resource", resourceJson.computeIfAbsent(log.getResource(), resource ->
                        {
                            Map<String, Object> resourceObject = resource.getAttributes().asMap().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getKey(), Map.Entry::getValue));
                            return objectMapper.valueToTree(resourceObject);
                        }))
                        .build();

                objectMapper.writeValue(baos, log);
                attachSender.send(new MetadataByteBuffer(metadata, ByteBuffer.wrap(baos.toByteArray())));
                baos.reset();
            }

/* For reference
            LogsRequestMarshaler request = LogsRequestMarshaler.create(logs);
            request.writeJsonTo(baos);
            attachSender.send(new MetadataByteBuffer(new Metadata.Builder().add("type", "log").build(), ByteBuffer.wrap(baos.toByteArray())));
            baos.reset();
*/
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
}
