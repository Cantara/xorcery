package com.exoreaction.xorcery.service.jersey.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public record JerseyClientConfiguration(Configuration configuration) {
    public Optional<JsonNode> getProperties() {
        return configuration.getJson("properties");
    }
}
