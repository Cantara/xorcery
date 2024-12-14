package dev.xorcery.opentelemetry.exporters.reactivestreams;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.reactivestreams.traces")
public class ReactiveStreamSpanProcessorFactory
        implements Factory<SpanProcessor>
{
    private final SpanProcessor spanProcessor;

    @Inject
    public ReactiveStreamSpanProcessorFactory(ReactiveStreamExporterService exporterService, Resource resource, Configuration configuration, LoggerContext loggerContext) {
        ReactiveStreamSpanExporter attachSpanExporter = new ReactiveStreamSpanExporter(exporterService, resource, loggerContext.getLogger(ReactiveStreamSpanExporter.class));
        spanProcessor = SimpleSpanProcessor.create(attachSpanExporter);
    }

    @Override
    @Singleton
    public SpanProcessor provide() {
        return spanProcessor;
    }

    @Override
    public void dispose(SpanProcessor spanProcessor) {
    }
}
