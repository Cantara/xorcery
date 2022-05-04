package com.exoreaction.reactiveservices.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public class Configuration
{
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
            yamlStream.close();
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
        return cfg(name, defaultValue, (config, n)->config.getBoolean(n, defaultValue));
    }

    public Configuration getConfiguration(String name) {
        JsonObject jsonObject = config.getJsonObject(name);
        if (jsonObject == null)
            jsonObject = Json.createObjectBuilder().build();  // Create empty configuration
        return new Configuration(jsonObject);
    }

    public Builder asBuilder() {
        return new Builder(Json.createObjectBuilder(config));
    }

    private <T> T cfg(String name, T defaultValue, BiFunction<JsonObject, String, T> lookup)
    {
        String[] names = name.split("\\.");
        JsonObject c = config;
        for (int i = 0; i < names.length-1; i++) {
            c = c.getJsonObject(names[i]);
            if (c == null)
                return defaultValue;
        }
        return lookup.apply(c, names[names.length-1]);
    }

    @Override
    public String toString() {
        Map<String, Object> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);
        StringWriter writer = new StringWriter();
        jsonWriterFactory.createWriter(writer).write(this.config);
        return writer.toString();
    }
}
