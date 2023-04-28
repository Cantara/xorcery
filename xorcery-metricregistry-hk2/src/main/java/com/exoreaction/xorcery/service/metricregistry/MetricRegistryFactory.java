package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.*;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricRegistryFactory
        implements Factory<MetricRegistry> {
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final Map<String, MetricRegistry> prefixedRegistries = new ConcurrentHashMap<>();
    private final InstantiationService instantiationService;
    private final JmxReporter reporter;

    @Inject
    public MetricRegistryFactory(InstantiationService instantiationService) {
        this.instantiationService = instantiationService;

        reporter = JmxReporter.forRegistry(metricRegistry)
                .inDomain("xorcery")
                .build();
        reporter.start();
    }

    @Override
    @PerLookup
    public MetricRegistry provide() {

        Injectee injectee = instantiationService.getInstantiationData().getParentInjectee();
        if (injectee != null && injectee.getInjecteeDescriptor() != null) {
            String name = injectee.getInjecteeDescriptor().getName();
            if (name != null) {
                return prefixedRegistries.computeIfAbsent(name, n ->
                {
                    MetricRegistry prefixedRegistry = new MetricRegistry();
                    metricRegistry.register(n, prefixedRegistry);
                    return prefixedRegistry;
                });
            }
        }

        return metricRegistry;
    }

    @Override
    public void dispose(MetricRegistry instance) {
        reporter.stop();
    }
}
