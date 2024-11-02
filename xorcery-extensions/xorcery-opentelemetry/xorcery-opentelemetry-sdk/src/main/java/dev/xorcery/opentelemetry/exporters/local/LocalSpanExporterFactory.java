package dev.xorcery.opentelemetry.exporters.local;


import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.local")
public class LocalSpanExporterFactory
        implements Factory<SpanProcessor> {
    private final SpanProcessor spanProcessor;

    @Inject
    public LocalSpanExporterFactory(LocalSpanExporter localSpanExporter) {
        spanProcessor = SimpleSpanProcessor.create(localSpanExporter);
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.local")
    public SpanProcessor provide() {
        return spanProcessor;
    }

    @Override
    public void dispose(SpanProcessor instance) {
    }
}
