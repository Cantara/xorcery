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
package com.exoreaction.xorcery.opentelemetry.sdk.exporters;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricProducer;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry")
public class MeterProviderFactory
        implements Factory<SdkMeterProvider> {
    private final SdkMeterProvider sdkMeterProvider;

    @Inject
    public MeterProviderFactory(Resource resource,
                                IterableProvider<MetricReader> metricReaders,
                                IterableProvider<MetricProducer> metricProducers) {
        var sdkMeterProviderBuilder = SdkMeterProvider.builder().setResource(resource);
        for (MetricReader metricReader : metricReaders) {
            sdkMeterProviderBuilder.registerMetricReader(metricReader);
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
