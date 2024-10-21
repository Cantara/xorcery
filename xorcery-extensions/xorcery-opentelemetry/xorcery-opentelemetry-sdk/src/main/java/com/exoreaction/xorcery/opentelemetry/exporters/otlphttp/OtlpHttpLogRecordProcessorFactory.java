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
package com.exoreaction.xorcery.opentelemetry.exporters.otlphttp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.sdk.common.export.RetryPolicy;
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
        builder.setConnectTimeout(otHttpConfiguration.getConnectTimeout());
        builder.setTimeout(otHttpConfiguration.getTimeout());
        builder.setRetryPolicy(RetryPolicy.getDefault());
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
