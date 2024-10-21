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
package com.exoreaction.xorcery.opentelemetry.exporters.jmx;

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

@Service
public class JMXMetricReaderFactory
        implements Factory<MetricReader> {
    private final MetricReader metricReader;

    @Inject
    public JMXMetricReaderFactory(Configuration configuration, LoggerContext loggerContext) {
        JMXExporterConfiguration jmxExporterConfiguration = JMXExporterConfiguration.get(configuration);

        JMXMetricExporter jmxMetricExporter = new JMXMetricExporter(AggregationTemporality.CUMULATIVE, loggerContext.getLogger(JMXMetricExporter.class));

        metricReader = PeriodicMetricReader.builder(jmxMetricExporter)
                .setInterval(jmxExporterConfiguration.getInterval())
                .build();
    }

    @Override
    @Singleton
    @Named("opentelemetry.exporters.jmx")
    public MetricReader provide() {
        return metricReader;
    }

    @Override
    public void dispose(MetricReader instance) {
    }
}
