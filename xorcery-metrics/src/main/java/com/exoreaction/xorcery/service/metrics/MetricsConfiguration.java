package com.exoreaction.xorcery.service.metrics;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

public record MetricsConfiguration(Configuration context)
        implements ServiceConfiguration {
    public String getSubscriberAuthority() {
        return context.getString("subscriber.authority").orElse(null);
    }

    public String getSubscriberStream() {
        return context.getString("subscriber.stream").orElseThrow();
    }

    public Configuration getSubscriberConfiguration() {
        return context.getConfiguration("subscriber.configuration");
    }

    public Configuration getPublisherConfiguration() {
        return context.getConfiguration("publisher.configuration");
    }

    public Optional<Iterable<JsonNode>> getFilters() {
        return context.getList("filter");
    }
}
