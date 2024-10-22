package com.exoreaction.xorcery.jsonschema.generator;

import com.exoreaction.xorcery.jsonschema.Definitions;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.Properties;
import com.exoreaction.xorcery.jsonschema.Types;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * This generator takes a best-effort approach to generate a JSON Schema based on
 * a JSON example document and optional resolved JSON example document (if using JsonResolver).
 *
 * It is recommended to use this in combination with JsonResolver and JsonMerger for best results, such as applying
 * any existing schema afterward using JsonMerger as it may have been manually edited.
 */
public class SchemaByExampleGenerator {

    JsonSchema.Builder rootSchema = new JsonSchema.Builder();

    public SchemaByExampleGenerator() {
        rootSchema
                .schema(JsonSchema.SCHEMA_2020_12)
                .type(Types.Object);
    }

    public SchemaByExampleGenerator id(String value) {
        rootSchema.id(value);
        return this;
    }

    public SchemaByExampleGenerator title(String value) {
        rootSchema.title(value);
        return this;
    }

    public JsonSchema generateJsonSchema(ObjectNode example)
    {
        return generateJsonSchema(example, example);
    }

    public JsonSchema generateJsonSchema(ObjectNode example, ObjectNode resolvedExample) {

        // Create top level definitions
        Properties.Builder properties = new Properties.Builder();
        Definitions.Builder definitions = new Definitions.Builder();
        for (Map.Entry<String, JsonNode> property : example.properties()) {
            if (property.getKey().startsWith("$"))
                continue;
            JsonNode defaultValue = example.get(property.getKey());
            JsonNode value = resolvedExample.get(property.getKey());
            JsonNode effectiveValue = value == null ? defaultValue : value;
            switch (effectiveValue.getNodeType()) {
                case ARRAY -> {
                }
                case BINARY -> {
                }
                case BOOLEAN -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .types(Types.Boolean, Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText() + (defaultValue.asText().contains("{{") ? " (" + effectiveValue.asText() + ")" : ""))
                            .build());
                }
                case MISSING -> {
                }
                case NULL -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: null")
                            .build());
                }
                case NUMBER -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .types(Types.Number, Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText() + (defaultValue.asText().contains("{{") ? " (" + effectiveValue.asText() + ")" : ""))
                            .build());
                }
                case OBJECT -> {
                    definitions.definition(property.getKey(), createSchema(rootSchema, (ObjectNode) defaultValue, (ObjectNode) effectiveValue));
                    properties.property(property.getKey(), new JsonSchema.Builder().ref("#/$defs/" + property.getKey()).build());
                }
                case POJO -> {
                }
                case STRING -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .type(Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText() + (defaultValue.asText().contains("{{") ? " (" + effectiveValue.asText() + ")" : ""))
                            .build());
                }
            }
        }
        rootSchema.properties(properties.build());
        rootSchema.definitions(definitions.build());
        return rootSchema.build();
    }

    private JsonSchema createSchema(JsonSchema.Builder rootSchema, ObjectNode configuration, ObjectNode resolvedConfiguration) {

        JsonSchema.Builder schema = new JsonSchema.Builder();
        schema.type(Types.Object);
        schema.additionalProperties(configuration.isEmpty());
        Properties.Builder properties = new Properties.Builder();
        for (Map.Entry<String, JsonNode> property : configuration.properties()) {

            JsonNode defaultValue = configuration.get(property.getKey());
            JsonNode value = resolvedConfiguration.get(property.getKey());
            JsonNode effectiveValue = value == null ? defaultValue : value;
            switch (effectiveValue.getNodeType()) {
                case ARRAY -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .types(Types.Array, Types.String)
                            .build());
                }
                case BINARY -> {
                }
                case BOOLEAN -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .types(Types.Boolean, Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText() + (defaultValue.asText().contains("{{") ? " (" + effectiveValue.asText() + ")" : ""))
                            .build());
                }
                case MISSING -> {
                }
                case NULL -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: null")
                            .build());
                }
                case NUMBER -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .types(Types.Number, Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText() + (defaultValue.asText().contains("{{") ? " (" + effectiveValue.asText() + ")" : ""))
                            .build());
                }
                case OBJECT -> {
                    properties.property(property.getKey(), createSchema(rootSchema,
                            defaultValue instanceof ObjectNode defaultOn
                                    ? defaultOn
                                    : effectiveValue instanceof ObjectNode effectiveOn
                                    ? effectiveOn
                                    : JsonNodeFactory.instance.objectNode(),
                            effectiveValue instanceof ObjectNode effectiveOn
                                    ? effectiveOn
                                    : JsonNodeFactory.instance.objectNode()));
                }
                case POJO -> {
                }
                case STRING -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .type(Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText() + (defaultValue.asText().contains("{{") ? " (" + effectiveValue.asText() + ")" : ""))
                            .build());
                }
            }
        }
        schema.properties(properties.build());
        return schema.build();
    }
}
