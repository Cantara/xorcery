package com.exoreaction.xorcery.opentelemetry.sdk.exporters.logging;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
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
