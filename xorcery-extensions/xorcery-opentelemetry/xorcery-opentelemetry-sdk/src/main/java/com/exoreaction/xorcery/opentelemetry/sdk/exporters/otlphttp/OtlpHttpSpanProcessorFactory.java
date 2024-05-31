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
package com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
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
        builder.setConnectTimeout(otHttpConfiguration.getConnectTimeout());
        builder.setTimeout(otHttpConfiguration.getTimeout());
        builder.setRetryPolicy(RetryPolicy.getDefault());
        otHttpConfiguration.getHeaders(secrets).forEach(builder::addHeader);
        otHttpConfiguration.getCompression().ifPresent(builder::setCompression);
        builder.setMeterProvider(meterProvider);

        OtlpHttpSpanExporter spanExporter = builder.build();

        spanProcessor = BatchSpanProcessor.builder(spanExporter)
                .setMeterProvider(meterProvider)
                .setExporterTimeout(otHttpConfiguration.getExporterTimeout())
                .setMaxExportBatchSize(otHttpConfiguration.getMaxExportBatchSize())
                .setMaxQueueSize(otHttpConfiguration.getMaxQueueSize())
                .setScheduleDelay(otHttpConfiguration.getScheduleDelay())
                .setExportUnsampledSpans(false)
                .build();
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
