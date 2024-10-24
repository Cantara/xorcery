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
package com.exoreaction.xorcery.opentelemetry.exporters.logging;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.logging")
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
