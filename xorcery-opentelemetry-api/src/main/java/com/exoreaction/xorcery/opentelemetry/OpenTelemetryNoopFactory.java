package com.exoreaction.xorcery.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry.noop")
public class OpenTelemetryNoopFactory
        implements Factory<OpenTelemetry> {

    @Inject
    public OpenTelemetryNoopFactory() {
    }

    @Override
    @Singleton
    @Named("opentelemetry.noop")
    public OpenTelemetry provide() {
        return OpenTelemetry.noop();
    }

    @Override
    public void dispose(OpenTelemetry instance) {

    }
}
