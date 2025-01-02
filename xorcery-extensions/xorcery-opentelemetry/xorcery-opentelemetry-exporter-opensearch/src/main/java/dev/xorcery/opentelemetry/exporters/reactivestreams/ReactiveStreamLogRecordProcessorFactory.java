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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.reactivestreams.logs")
public class ReactiveStreamLogRecordProcessorFactory
        implements Factory<LogRecordProcessor>
{
    private final LogRecordProcessor logRecordProcessor;

    @Inject
    public ReactiveStreamLogRecordProcessorFactory(ReactiveStreamExporterService exporterService, Resource resource) {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LogRecordData.class, new LogRecordDataSerializer());
        objectMapper.registerModule(module);

        LogRecordExporter logRecordExporter = new ReactiveStreamLogRecordExporter(exporterService, resource, objectMapper);
        this.logRecordProcessor = SimpleLogRecordProcessor.create(logRecordExporter);
    }

    @Override
    @Singleton
    public LogRecordProcessor provide() {
        return logRecordProcessor;
    }

    @Override
    public void dispose(LogRecordProcessor instance) {
        logRecordProcessor.close();
    }

}
