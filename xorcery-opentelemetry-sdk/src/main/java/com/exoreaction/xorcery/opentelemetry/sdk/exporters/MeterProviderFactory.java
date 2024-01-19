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

@Service(name = "opentelemetry.exporters")
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
