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
package dev.xorcery.opentelemetry.exporters.otlphttp;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.secrets.Secrets;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.otlp.http")
public class OtlpHttpMetricExporterFactory
        implements Factory<OtlpHttpMetricExporter> {
    private final OtlpHttpMetricExporter metricExporter;

    @Inject
    public OtlpHttpMetricExporterFactory(Configuration configuration, Secrets secrets) {
        OtlpHttpConfiguration otHttpConfiguration = OtlpHttpConfiguration.get(configuration);

        OtlpHttpMetricExporterBuilder builder = OtlpHttpMetricExporter.builder();
        builder.setEndpoint(otHttpConfiguration.getMetricsEndpoint());
        builder.setAggregationTemporalitySelector(switch (otHttpConfiguration.getAggregationTemporality()) {
            case alwaysCumulative -> AggregationTemporalitySelector.alwaysCumulative();
            case deltaPreferred -> AggregationTemporalitySelector.deltaPreferred();
            case lowMemory -> AggregationTemporalitySelector.lowMemory();
        });
        builder.setConnectTimeout(otHttpConfiguration.getConnectTimeout());
        builder.setTimeout(otHttpConfiguration.getTimeout());
        builder.setRetryPolicy(RetryPolicy.getDefault());
        otHttpConfiguration.getHeaders(secrets).forEach(builder::addHeader);
        otHttpConfiguration.getCompression().ifPresent(builder::setCompression);
        metricExporter = builder.build();
    }

    @Override
    @Singleton
    public OtlpHttpMetricExporter provide() {
        return metricExporter;
    }

    @Override
    public void dispose(OtlpHttpMetricExporter instance) {
    }
}
