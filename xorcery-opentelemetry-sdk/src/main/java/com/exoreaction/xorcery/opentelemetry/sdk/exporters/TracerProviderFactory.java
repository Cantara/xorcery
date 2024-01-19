package com.exoreaction.xorcery.opentelemetry.sdk.exporters;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry.exporters")
public class TracerProviderFactory
        implements Factory<SdkTracerProvider> {
    private final SdkTracerProvider sdkTracerProvider;

    @Inject
    public TracerProviderFactory(Resource resource,
                                 IterableProvider<SpanProcessor> spanProcessors) {
        var sdkTracerProviderBuilder = SdkTracerProvider.builder()
                .setResource(resource);
        for (SpanProcessor spanProcessor : spanProcessors) {
            sdkTracerProviderBuilder.addSpanProcessor(spanProcessor);
        }
        sdkTracerProvider = sdkTracerProviderBuilder.build();
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
