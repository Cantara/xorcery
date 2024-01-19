package com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class OtlpHttpLogRecordProcessorFactory
        implements Factory<LogRecordProcessor> {
    private final BatchLogRecordProcessor logRecordProcessor;

    @Inject
    public OtlpHttpLogRecordProcessorFactory(Configuration configuration,
                                             Secrets secrets,
                                             SdkMeterProvider meterProvider) {
        OtlpHttpConfiguration otHttpConfiguration = OtlpHttpConfiguration.get(configuration);

        OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder();
        builder.setEndpoint(otHttpConfiguration.getLogsEndpoint());
        otHttpConfiguration.getHeaders(secrets).forEach(builder::addHeader);
        otHttpConfiguration.getCompression().ifPresent(builder::setCompression);
        builder.setMeterProvider(meterProvider);
        LogRecordExporter logRecordExporter = builder.build();

        logRecordProcessor = BatchLogRecordProcessor.builder(logRecordExporter)
                .setMeterProvider(meterProvider)
                .setScheduleDelay(otHttpConfiguration.getScheduleDelay())
                .setExporterTimeout(otHttpConfiguration.getExporterTimeout())
                .setMaxExportBatchSize(otHttpConfiguration.getMaxExportBatchSize())
                .setMaxQueueSize(otHttpConfiguration.getMaxQueueSize())
                .build();
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.otlp.http")
    public LogRecordProcessor provide() {
        return logRecordProcessor;
    }

    @Override
    public void dispose(LogRecordProcessor instance) {
    }
}
