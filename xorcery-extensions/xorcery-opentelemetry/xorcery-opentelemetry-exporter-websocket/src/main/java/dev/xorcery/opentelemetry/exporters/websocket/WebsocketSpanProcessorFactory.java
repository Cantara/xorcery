package dev.xorcery.opentelemetry.exporters.websocket;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.websocket.traces")
public class WebsocketSpanProcessorFactory
        implements Factory<SpanProcessor>
{
    private final SpanProcessor spanProcessor;

    @Inject
    public WebsocketSpanProcessorFactory(WebsocketExporterService exporterService, Configuration configuration, LoggerContext loggerContext) {
        WebsocketSpanExporter attachSpanExporter = new WebsocketSpanExporter(exporterService, loggerContext.getLogger(WebsocketSpanExporter.class));
        spanProcessor = SimpleSpanProcessor.create(attachSpanExporter);
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.websocket.traces")
    public SpanProcessor provide() {
        return spanProcessor;
    }

    @Override
    public void dispose(SpanProcessor spanProcessor) {
    }
}
