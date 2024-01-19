package com.exoreaction.xorcery.opentelemetry.sdk.exporters.logging;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class LoggingMetricReaderFactory
        implements Factory<MetricReader> {
    private final MetricReader metricReader;

    @Inject
    public LoggingMetricReaderFactory(Configuration configuration) {
        LoggingConfiguration loggingConfiguration = LoggingConfiguration.get(configuration);

        LoggingMetricExporter loggingMetricExporter = LoggingMetricExporter.create();

        metricReader = PeriodicMetricReader.builder(loggingMetricExporter)
                .setInterval(loggingConfiguration.getInterval())
                .build();
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.logging")
    public MetricReader provide() {
        return metricReader;
    }

    @Override
    public void dispose(MetricReader instance) {
    }
}
