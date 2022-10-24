package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.JsonMerger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public record PropertiesConfigurationBuilder(Configuration.Builder builder) {
    public void addProperties(InputStream propertiesStream) throws UncheckedIOException {
        try (propertiesStream) {
            ObjectNode properties = (ObjectNode) new ObjectMapper(new JavaPropsFactory()).readTree(propertiesStream);
            new JsonMerger().merge(builder.builder(), properties);
        } catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    public void addProperties(String propertiesString) throws UncheckedIOException {
        try {
            ObjectNode properties = (ObjectNode) new ObjectMapper(new JavaPropsFactory()).readTree(propertiesString);
            new JsonMerger().merge(builder.builder(), properties);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
