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
package dev.xorcery.opentelemetry.sdk;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricProducer;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry")
public class MeterProviderFactory
        implements Factory<SdkMeterProvider> {
    private final SdkMeterProvider sdkMeterProvider;

    @Inject
    public MeterProviderFactory(Resource resource,
                                Configuration configuration,
                                ServiceLocator serviceLocator,
                                IterableProvider<MetricExporter> metricExporters,
                                IterableProvider<MetricProducer> metricProducers) {

        OpenTelemetryConfiguration openTelemetryConfiguration = OpenTelemetryConfiguration.get(configuration);
        var sdkMeterProviderBuilder = SdkMeterProvider.builder().setResource(resource);
        for (MetricExporter metricExporter : metricExporters) {
            PeriodicMetricReader periodicMetricReader = PeriodicMetricReader.builder(metricExporter)
                    .setInterval(openTelemetryConfiguration.getMetricReaderInterval())
                    .build();
            sdkMeterProviderBuilder.registerMetricReader(periodicMetricReader);
        }
        for (MetricProducer metricProducer : metricProducers) {
            sdkMeterProviderBuilder.registerMetricProducer(metricProducer);
        }

        sdkMeterProvider = sdkMeterProviderBuilder.build();
    }

    @Override
    @Singleton
    public SdkMeterProvider provide() {
        return sdkMeterProvider;
    }

    @Override
    public void dispose(SdkMeterProvider instance) {
        instance.close();
    }
}
