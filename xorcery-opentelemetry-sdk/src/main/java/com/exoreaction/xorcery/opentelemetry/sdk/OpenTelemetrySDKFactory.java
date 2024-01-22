package com.exoreaction.xorcery.opentelemetry.sdk;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
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

@Service(name = "opentelemetry")
@RunLevel(0)
public class OpenTelemetrySDKFactory
    implements Factory<OpenTelemetry>
{

    private final OpenTelemetrySdk openTelemetry;

    @Inject
    public OpenTelemetrySDKFactory(Configuration configuration,
                                   SdkTracerProvider tracerProvider,
                                   SdkMeterProvider meterProvider,
                                   Provider<SdkLoggerProvider> loggerProvider) {

        OpenTelemetryConfiguration openTelemetryConfiguration = OpenTelemetryConfiguration.get(configuration);

        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider.get())
                .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
                .build();

        if (openTelemetryConfiguration.isInstall())
        {
            GlobalOpenTelemetry.set(openTelemetry);
        }
    }

    @Override
    @Singleton
    @Named("opentelemetry")
    public OpenTelemetry provide() {
        return openTelemetry;
    }

    @Override
    public void dispose(OpenTelemetry instance) {
        openTelemetry.close();
    }
}
