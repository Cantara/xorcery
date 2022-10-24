package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.JsonMerger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public record YamlConfigurationBuilder(Configuration.Builder builder) {
    public void addYaml(InputStream yamlStream) throws UncheckedIOException {
        try (yamlStream) {
            ObjectNode yaml = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(yamlStream);
            new JsonMerger().merge(builder.builder(), yaml);
        } catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    public void addYaml(String yamlString) throws UncheckedIOException {
        try {
            ObjectNode yaml = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(yamlString);
            new JsonMerger().merge(builder.builder(), yaml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
