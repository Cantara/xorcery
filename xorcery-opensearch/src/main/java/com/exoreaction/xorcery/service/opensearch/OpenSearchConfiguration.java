package com.exoreaction.xorcery.service.opensearch;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.fasterxml.jackson.databind.JsonNode;

public record OpenSearchConfiguration(Configuration configuration) {
    public JsonNode getComponentTemplates() {
        return configuration.getJson("componentTemplates").orElseThrow(() ->
                new IllegalStateException("Missing opensearch.componentTemplates configuration"));
    }

    public JsonNode getIndexTemplates() {
        return configuration.getJson("indexTemplates").orElseThrow(() ->
                new IllegalStateException("Missing opensearch.indexTemplates configuration"));
    }

    public boolean isDeleteOnExit() {
        return configuration.getBoolean("opensearch.deleteOnExit").orElse(false);
    }
}
