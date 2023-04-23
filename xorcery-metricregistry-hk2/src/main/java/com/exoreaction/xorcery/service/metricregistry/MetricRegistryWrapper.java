package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.MetricRegistry;

public class MetricRegistryWrapper {

    private final MetricRegistry metricRegistry;

    public MetricRegistryWrapper(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public MetricRegistry metricRegistry() {
        return metricRegistry;
    }
}
