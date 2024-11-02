package dev.xorcery.opentelemetry.sdk;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;

public record OpenTelemetryImpl(TracerProvider tracerProvider,
                                MeterProvider meterProvider,
                                LoggerProvider sdkLoggerProvider,
                                ContextPropagators contextPropagators)
    implements OpenTelemetry
{
    @Override
    public TracerProvider getTracerProvider() {
        return tracerProvider;
    }

    @Override
    public MeterProvider getMeterProvider() {
        return meterProvider;
    }

    @Override
    public LoggerProvider getLogsBridge() {
        return sdkLoggerProvider;
    }

    @Override
    public ContextPropagators getPropagators() {
        return contextPropagators;
    }
}
