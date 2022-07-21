package com.exoreaction.xorcery.configuration;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.json.VariableResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public record Configuration(ObjectNode json)
    implements JsonElement
{
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

        public Builder add(String name, long value) {
            return add(name, builder.numberNode(value));
        }

        public Builder addYaml(InputStream yamlStream) throws IOException {
            try (yamlStream) {
                ObjectNode yaml = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(yamlStream);
                new JsonMerger().merge(builder, yaml);
                return this;
            }
        }

        public Builder addYaml(String yamlString) throws IOException {
                ObjectNode yaml = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(yamlString);
                new JsonMerger().merge(builder, yaml);
                return this;
        }

        public Builder addSystemProperties(String nodeName) {
            ObjectNode system = builder.objectNode();
            for (Map.Entry<Object, Object> systemProperty : System.getProperties().entrySet()) {
                system.set(systemProperty.getKey().toString().replace('.','_'), builder.textNode(systemProperty.getValue().toString()));
            }
            builder.set(nodeName, system);
            return this;
        }

        public Builder addEnvironmentVariables(String nodeName) {
            ObjectNode env = builder.objectNode();
            System.getenv().forEach((key, value) -> env.set(key.replace('.','_'), env.textNode(value)));
            builder.set(nodeName, env);
            return this;
        }

        public Configuration build() {
            // Resolve any references
            return new Configuration(new VariableResolver().apply(builder, builder));
        }
    }

    public Configuration getConfiguration(String name) {
        return getJson(name)
                .map(ObjectNode.class::cast).map(Configuration::new)
                .orElseGet(() -> new Configuration(JsonNodeFactory.instance.objectNode()));
    }

    public List<Configuration> getConfigurations(String name) {
        return getJson(name)
                .map(ArrayNode.class::cast)
                .map(a -> JsonElement.getValuesAs(a, Configuration::new))
                .orElseGet(Collections::emptyList);
    }

    public Builder asBuilder() {
        return new Builder(json);
    }
}

