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

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry")
public class TracerProviderFactory
        implements Factory<SdkTracerProvider> {
    private final SdkTracerProvider sdkTracerProvider;

    @Inject
    public TracerProviderFactory(Resource resource,
                                 RuleBasedSampler ruleBasedSampler,
                                 IterableProvider<SpanProcessor> spanProcessors) {
        var sdkTracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource);
        for (SpanProcessor spanProcessor : spanProcessors) {
            sdkTracerProviderBuilder.addSpanProcessor(spanProcessor);
        }

        Sampler sampler = Sampler.parentBasedBuilder(ruleBasedSampler)
                .build();
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
