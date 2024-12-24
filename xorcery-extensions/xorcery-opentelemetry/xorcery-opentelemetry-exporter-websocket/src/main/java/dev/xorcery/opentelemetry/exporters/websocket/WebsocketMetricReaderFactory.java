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
package dev.xorcery.opentelemetry.exporters.websocket;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import jakarta.inject.Inject;
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
    public MetricReader provide() {
        return metricReader;
    }

    @Override
    public void dispose(MetricReader instance) {
    }
}
