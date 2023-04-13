package com.exoreaction.xorcery.service.opensearch;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public record OpenSearchConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public JsonNode getComponentTemplates() {
        return context.getJson("componentTemplates").orElseThrow(() ->
                new IllegalArgumentException("Missing opensearch.componentTemplates configuration"));
    }

    public JsonNode getIndexTemplates() {
        return context.getJson("indexTemplates").orElseThrow(() ->
                new IllegalArgumentException("Missing opensearch.indexTemplates configuration"));
    }

    public boolean isDeleteOnExit() {
        return context.getBoolean("deleteOnExit").orElse(false);
    }

    public URI getURL() {
        return context.getURI("url").orElseThrow(()->new IllegalArgumentException("Missing opensearch.url configuration"));
    }

    public Optional<List<OpenSearchService.Publisher>> getPublishers() {
        return context.getObjectListAs("publishers", OpenSearchService.Publisher::new);
    }
}
