package dev.xorcery.opentelemetry.exporters.reactivestreams;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.websocket.metrics")
public class ReactiveStreamMetricReaderFactory
        implements Factory<MetricReader> {
    private final MetricReader metricReader;
    private final Resource resource;

    @Inject
    public ReactiveStreamMetricReaderFactory(
            ReactiveStreamExporterService exporterService,
            Resource resource,
            Configuration configuration,
            LoggerContext loggerContext) {
        this.resource = resource;
        ReactiveStreamExporterMetricsConfiguration reactiveStreamExporterMetricsConfiguration = ReactiveStreamExporterMetricsConfiguration.get(configuration);
        ReactiveStreamMetricExporter reactiveStreamMetricExporter = new ReactiveStreamMetricExporter(exporterService, AggregationTemporality.CUMULATIVE, resource, loggerContext.getLogger(ReactiveStreamMetricExporter.class));
        metricReader = PeriodicMetricReader.builder(reactiveStreamMetricExporter)
                .setInterval(reactiveStreamExporterMetricsConfiguration.getInterval())
                .build();
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.websocket.metrics")
    public MetricReader provide() {
        return metricReader;
    }

    @Override
    public void dispose(MetricReader instance) {
    }
}
