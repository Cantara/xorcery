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
package dev.xorcery.opentelemetry.exporters.logging;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.logging")
public class LoggingSpanExporterFactory
        implements Factory<SpanProcessor> {
    private final SpanProcessor spanProcessor;

    @Inject
    public LoggingSpanExporterFactory() {
        LoggingSpanExporter spanExporter = LoggingSpanExporter.create();
        spanProcessor = SimpleSpanProcessor.create(spanExporter);
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.logging")
    public SpanProcessor provide() {
        return spanProcessor;
    }

    @Override
    public void dispose(SpanProcessor instance) {
    }
}
