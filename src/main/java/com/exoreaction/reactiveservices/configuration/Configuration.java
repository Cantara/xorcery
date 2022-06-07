package com.exoreaction.reactiveservices.configuration;

import com.exoreaction.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.BiFunction;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public record Configuration(ObjectNode config) {

    public record Builder(ObjectNode builder) {

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder add(String name, JsonNode value) {
            builder.set(name, value);
            return this;
        }

        public Builder add(String name, String value) {
            return add(name, builder.textNode(value));
        }

        public Builder addYaml(InputStream yamlStream) throws IOException {
            try {
                ObjectNode current = builder;
                ObjectNode yaml = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(yamlStream);

                if (current.isEmpty()) {
                    return new Builder(yaml);
                } else {
                    return new Builder(merge(current, yaml));
                }
            } finally {
                yamlStream.close();
            }
        }

        private ObjectNode merge(ObjectNode current, ObjectNode yaml) {
            ObjectNode merged = JsonNodeFactory.instance.objectNode();

            Iterator<Map.Entry<String, JsonNode>> fields = current.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getValue() instanceof ObjectNode object) {
                    merged.set(entry.getKey(), merge(object, (ObjectNode) yaml.path(entry.getKey())));
                } else {
                    JsonNode override = yaml.get(entry.getKey());
                    merged.set(entry.getKey(), override != null ? override : entry.getValue());
                }

            }
            return merged;
        }

        public Configuration build() {
            return new Configuration(builder);
        }
    }

    public Optional<String> getString(String name) {
        return cfg(name, (config, n) -> Optional.ofNullable(config.get(n))
                .map(JsonNode::textValue));
    }

    public Optional<URI> getURI(String name) {

        return cfg(name, (config, n) -> Optional.ofNullable(config.get(n))
                .map(JsonNode::textValue).map(URI::create));
    }

    public Optional<Integer> getInteger(String name) {
        return cfg(name, (config, n) -> Optional.ofNullable(config.get(n))
                .map(JsonNode::intValue));
    }

    public Optional<Boolean> getBoolean(String name) {
        return cfg(name, (config, n) -> Optional.ofNullable(config.get(n))
                .map(JsonNode::asBoolean));
    }

    public Optional<Iterable<JsonNode>> getList(String name) {
        return cfg(name, (config, n) -> Optional.ofNullable(config.get(n))
                .map(ArrayNode.class::cast));
    }

    public Map<String, JsonNode> asMap() {
        return JsonNodes.asMap(config);
    }

    public Configuration getConfiguration(String name) {
        return cfg(name, (config, n) -> Optional.ofNullable(config.get(n))
                .map(ObjectNode.class::cast).map(Configuration::new))
                .orElseGet(() -> new Configuration(JsonNodeFactory.instance.objectNode()));
    }

    public List<Configuration> getConfigurations(String name) {
        return cfg(name, (config, n) -> Optional.ofNullable(config.get(n))
                .map(ArrayNode.class::cast)
                .map(a -> JsonNodes.getValuesAs(a, Configuration::new)))
                .orElseGet(Collections::emptyList);
    }

    public Builder asBuilder() {
        return new Builder(config);
    }

    private <T> Optional<T> cfg(String name, BiFunction<ObjectNode, String, Optional<T>> lookup) {
        String[] names = name.split("\\.");
        ObjectNode c = config;
        for (int i = 0; i < names.length - 1; i++) {
            c = (ObjectNode) c.get(names[i]);
            if (c == null)
                return Optional.empty();
        }
        return lookup.apply(c, names[names.length - 1]);
    }

    @Override
    public String toString() {
        return config.toPrettyString();
    }
}
