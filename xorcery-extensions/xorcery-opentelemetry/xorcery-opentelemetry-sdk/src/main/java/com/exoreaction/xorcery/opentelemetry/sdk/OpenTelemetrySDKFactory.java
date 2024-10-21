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
package com.exoreaction.xorcery.opentelemetry.sdk;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

@Service(name = "opentelemetry")
@RunLevel(0)
public class OpenTelemetrySDKFactory
        implements Factory<OpenTelemetry> {
    private final OpenTelemetryImpl openTelemetry;

    @Inject
    public OpenTelemetrySDKFactory(Configuration configuration,
                                   SdkTracerProvider tracerProvider,
                                   SdkMeterProvider meterProvider,
                                   Provider<SdkLoggerProvider> loggerProvider) {
        OpenTelemetryConfiguration openTelemetryConfiguration = OpenTelemetryConfiguration.get(configuration);

        // This allows us to turn off individual instrumentation scopes
        List<String> excludes = openTelemetryConfiguration.getExcludedMeters();
        List<String> includes = openTelemetryConfiguration.getIncludedMeters();

        MeterProvider filteredMeterProvider = instrumentationScopeName -> excludes.isEmpty()
                ? (includes.isEmpty()
                ? meterProvider.meterBuilder(instrumentationScopeName)
                : (isIncluded(includes, instrumentationScopeName)
                ? meterProvider.meterBuilder(instrumentationScopeName)
                : MeterProvider.noop().meterBuilder(instrumentationScopeName)))
                : (isExcluded(excludes, instrumentationScopeName)
                ? (isIncluded(includes, instrumentationScopeName)
                ? meterProvider.meterBuilder(instrumentationScopeName)
                : MeterProvider.noop().meterBuilder(instrumentationScopeName))
                : meterProvider.meterBuilder(instrumentationScopeName));

        openTelemetry = new OpenTelemetryImpl(
                tracerProvider,
                filteredMeterProvider,
                loggerProvider.get(),
                ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())));

        if (openTelemetryConfiguration.isInstall()) {
            GlobalOpenTelemetry.set(openTelemetry);
        }
    }

    private boolean isExcluded(List<String> excludes, String instrumentationScopeName) {
        for (String exclude : excludes) {
            if (instrumentationScopeName.contains(exclude))
                return true;
        }
        return false;
    }

    private boolean isIncluded(List<String> includes, String instrumentationScopeName) {
        for (String include : includes) {
            if (instrumentationScopeName.contains(include))
                return true;
        }
        return false;
    }

    @Override
    @Singleton
    @Named("opentelemetry")
    public OpenTelemetry provide() {
        return openTelemetry;
    }

    @Override
    public void dispose(OpenTelemetry instance) {
    }
}
