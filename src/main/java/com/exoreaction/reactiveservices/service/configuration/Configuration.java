package com.exoreaction.reactiveservices.service.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public class Configuration {
    public static class Builder {
        private JsonObjectBuilder builder;

        public Builder() {
            this(Json.createObjectBuilder());
        }

        public Builder(JsonObjectBuilder builder) {
            this.builder = builder;
        }

        public Builder addYaml(InputStream yamlStream) throws IOException {
            JsonObject current = builder.build();
            Map<String, Object> yaml = new ObjectMapper(new YAMLFactory()).readValue(yamlStream, Map.class);
            if (current.isEmpty()) {
                builder = Json.createObjectBuilder(yaml);
            } else {
                builder = merge(current, yaml);
            }

            return this;
        }

        private JsonObjectBuilder merge(JsonObject current, Map<String, Object> yaml) {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            for (Map.Entry<String, JsonValue> entry : current.entrySet()) {
                if (entry.getValue() instanceof JsonObject) {
                    builder.add(entry.getKey(), merge((JsonObject) entry.getValue(), (Map<String, Object>) yaml.get(entry.getKey())).build());
                } else {
                    builder.add(entry.getKey(), (JsonValue) yaml.getOrDefault(entry.getKey(), entry.getValue()));
                }
            }
            return objectBuilder;
        }

        public Configuration build() {
            return new Configuration(builder.build());
        }
    }

    private final JsonObject config;

    private Configuration(JsonObject config) {
        this.config = config;
    }

    public String getString(String name, String defaultValue) {
        return config.getString(name, defaultValue);
    }

    public URI getURI(String name, URI defaultValue) {

        return Optional.ofNullable(config.getString(name)).map(URI::create).orElse(defaultValue);
    }

    public int getInt(String name, int defaultValue) {
        return config.getInt(name, defaultValue);
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        return config.getBoolean(name, defaultValue);
    }

    public Configuration getConfiguration(String name) {
        return new Configuration(config.getJsonObject(name));
    }

    public Builder asBuilder() {
        return new Builder(Json.createObjectBuilder(config));
    }
}
