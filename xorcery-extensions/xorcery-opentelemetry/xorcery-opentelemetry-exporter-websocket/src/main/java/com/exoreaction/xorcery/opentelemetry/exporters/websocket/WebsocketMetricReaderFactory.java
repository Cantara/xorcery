package com.exoreaction.xorcery.opentelemetry.exporters.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.websocket.metrics")
public class WebsocketMetricReaderFactory
        implements Factory<MetricReader> {
    private final MetricReader metricReader;

    @Inject
    public WebsocketMetricReaderFactory(WebsocketExporterService exporterService, Configuration configuration, LoggerContext loggerContext) {
        WebsocketExporterMetricsConfiguration websocketExporterMetricsConfiguration = WebsocketExporterMetricsConfiguration.get(configuration);
        WebsocketMetricExporter websocketMetricExporter = new WebsocketMetricExporter(exporterService, AggregationTemporality.CUMULATIVE, loggerContext.getLogger(WebsocketMetricExporter.class));
        metricReader = PeriodicMetricReader.builder(websocketMetricExporter)
                .setInterval(websocketExporterMetricsConfiguration.getInterval())
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
