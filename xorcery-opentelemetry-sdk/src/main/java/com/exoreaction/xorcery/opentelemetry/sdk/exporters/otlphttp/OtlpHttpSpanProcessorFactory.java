package com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class OtlpHttpSpanProcessorFactory
        implements Factory<SpanProcessor> {
    private final SpanProcessor spanProcessor;

    @Inject
    public OtlpHttpSpanProcessorFactory(Configuration configuration,
                                        Secrets secrets,
                                        SdkMeterProvider meterProvider) {
        OtlpHttpConfiguration otHttpConfiguration = OtlpHttpConfiguration.get(configuration);

        OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder();
        builder.setEndpoint(otHttpConfiguration.getTracesEndpoint());
        otHttpConfiguration.getHeaders(secrets).forEach(builder::addHeader);
        otHttpConfiguration.getCompression().ifPresent(builder::setCompression);
        builder.setMeterProvider(meterProvider);

        OtlpHttpSpanExporter spanExporter = builder.build();

        spanProcessor = SimpleSpanProcessor.create(spanExporter);
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.otlp.http")
    public SpanProcessor provide() {
        return spanProcessor;
    }

    @Override
    public void dispose(SpanProcessor instance) {
    }
}
