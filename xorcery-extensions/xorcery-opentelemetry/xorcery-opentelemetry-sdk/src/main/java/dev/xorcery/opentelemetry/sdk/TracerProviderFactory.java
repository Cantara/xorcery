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
import dev.xorcery.hk2.Services;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry")
public class TracerProviderFactory
        implements Factory<SdkTracerProvider> {
    private final SdkTracerProvider sdkTracerProvider;

    @Inject
    public TracerProviderFactory(Resource resource,
                                 Configuration configuration,
                                 SdkMeterProvider meterProvider,
                                 @Optional Sampler parentSampler,
                                 ServiceLocator serviceLocator) {
        OpenTelemetryConfiguration openTelemetryConfiguration = OpenTelemetryConfiguration.get(configuration);
        var sdkTracerProviderBuilder = SdkTracerProvider.builder().setResource(resource);
        for (SpanExporter spanExporter : Services.allOfTypeRanked(serviceLocator, SpanExporter.class)
                .map(ServiceHandle::getService)
                .toList()) {
            SpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter)
                    .setMeterProvider(meterProvider)
                    .setExporterTimeout(openTelemetryConfiguration.getSpanExporterTimeout())
                    .setMaxExportBatchSize(openTelemetryConfiguration.getSpanMaxExportBatchSize())
                    .setMaxQueueSize(openTelemetryConfiguration.getSpanMaxQueueSize())
                    .setScheduleDelay(openTelemetryConfiguration.getSpanScheduleDelay())
                    .setExportUnsampledSpans(false)
                    .build();
            sdkTracerProviderBuilder.addSpanProcessor(spanProcessor);
        }

        Sampler sampler = parentSampler != null
                ? Sampler.parentBasedBuilder(parentSampler)
                .setLocalParentSampled(parentSampler)
                .setRemoteParentSampled(parentSampler)
                .build()
                : Sampler.alwaysOn();
        sdkTracerProvider = sdkTracerProviderBuilder
                .setSampler(sampler)
                .build();
    }

    @Override
    @Singleton
    public SdkTracerProvider provide() {
        return sdkTracerProvider;
    }

    @Override
    public void dispose(SdkTracerProvider instance) {
        sdkTracerProvider.close();
    }
}
