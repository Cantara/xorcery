package com.exoreaction.xorcery.configuration.providers;

import com.exoreaction.xorcery.configuration.spi.ConfigurationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.util.Optional;

public class EnvironmentVariablesConfigurationProvider
        implements ConfigurationProvider {

    @Override
    public JsonNode getJson(String name) {
        return Optional.ofNullable(System.getenv(name))
                .<JsonNode>map(JsonNodeFactory.instance::textNode)
                .orElseGet(MissingNode::getInstance);
    }

    @Override
    public String toString() {
        return "Environment variables";
    }
}
