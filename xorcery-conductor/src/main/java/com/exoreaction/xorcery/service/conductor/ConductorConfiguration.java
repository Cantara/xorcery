package com.exoreaction.xorcery.service.conductor;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

public record ConductorConfiguration(Configuration context)
        implements ServiceConfiguration {
    public Iterable<JsonNode> getTemplates() {
        return context.getList("templates").orElseThrow(() ->
                new IllegalStateException("Missing conductor.templates configuration"));
    }
}
