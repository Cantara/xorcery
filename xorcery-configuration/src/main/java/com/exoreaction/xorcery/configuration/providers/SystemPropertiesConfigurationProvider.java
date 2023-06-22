package com.exoreaction.xorcery.configuration.providers;

import com.exoreaction.xorcery.configuration.spi.ConfigurationProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.UncheckedIOException;
import java.util.Optional;

public class SystemPropertiesConfigurationProvider
        implements ConfigurationProvider {

    private final YAMLMapper yamlMapper;

    public SystemPropertiesConfigurationProvider() {
        yamlMapper = new YAMLMapper();
    }

    @Override
    public JsonNode getJson(String name) {
        return Optional.ofNullable(System.getProperty(name.replace('_', '.')))
                .map(value ->
                {
                    try {
                        return yamlMapper.readTree(value);
                    } catch (JsonProcessingException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .orElseGet(MissingNode::getInstance);
    }

    @Override
    public String toString() {
        return "System properties";
    }
}
