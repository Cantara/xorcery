package com.exoreaction.xorcery.configuration.jsonschema;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.jsonschema.Definitions;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.Properties;
import com.exoreaction.xorcery.jsonschema.Types;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public class ConfigurationSchemaBuilder {

    JsonSchema.Builder rootSchema = new JsonSchema.Builder();

    public ConfigurationSchemaBuilder() {
        rootSchema
                .schema(JsonSchema.SCHEMA_2020_12)
                .type(Types.Object);
    }

    public ConfigurationSchemaBuilder id(String value) {
        rootSchema.id(value);
        return this;
    }

    public ConfigurationSchemaBuilder title(String value) {
        rootSchema.title(value);
        return this;
    }

    public JsonSchema generateJsonSchema(ConfigurationBuilder configurationBuilder) {
        return generateJsonSchema(configurationBuilder.builder().builder(), configurationBuilder.build().json());
    }

    public JsonSchema generateJsonSchema(ObjectNode configuration, ObjectNode resolvedConfiguration) {

        // Create top level definitions
        Properties.Builder properties = new Properties.Builder();
        Definitions.Builder definitions = new Definitions.Builder();
        for (Map.Entry<String, JsonNode> property : resolvedConfiguration.properties()) {

            JsonNode defaultValue = configuration.get(property.getKey());
            switch (property.getValue().getNodeType()) {
                case ARRAY -> {
                }
                case BINARY -> {
                }
                case BOOLEAN -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .with(b -> b.builder().set("type", b.builder().arrayNode().add(Types.Boolean.name().toLowerCase()).add(Types.String.name().toLowerCase())))
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asBoolean())
                            .build());
                }
                case MISSING -> {
                }
                case NULL -> {
                }
                case NUMBER -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .with(b -> b.builder().set("type", b.builder().arrayNode().add(Types.Number.name().toLowerCase()).add(Types.String.name().toLowerCase())))
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText())
                            .build());
                }
                case OBJECT -> {
                    definitions.definition(property.getKey(), createSchema(rootSchema, (ObjectNode) defaultValue, (ObjectNode) property.getValue()));
                    properties.property(property.getKey(), new JsonSchema.Builder().ref("#/$defs/"+property.getKey()).build());
                }
                case POJO -> {
                }
                case STRING -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .type(Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText())
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
        Properties.Builder properties = new Properties.Builder();
        for (Map.Entry<String, JsonNode> property : resolvedConfiguration.properties()) {

            JsonNode defaultValue = configuration.get(property.getKey());
            switch (property.getValue().getNodeType()) {
                case ARRAY -> {
                }
                case BINARY -> {
                }
                case BOOLEAN -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .with(b -> b.builder().set("type", b.builder().arrayNode().add(Types.Boolean.name().toLowerCase()).add(Types.String.name().toLowerCase())))
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asBoolean())
                            .build());
                }
                case MISSING -> {
                }
                case NULL -> {
                }
                case NUMBER -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .with(b -> b.builder().set("type", b.builder().arrayNode().add(Types.Number.name().toLowerCase()).add(Types.String.name().toLowerCase())))
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText())
                            .build());
                }
                case OBJECT -> {
                    properties.property(property.getKey(), createSchema(rootSchema, (ObjectNode) defaultValue, (ObjectNode) property.getValue()));
                }
                case POJO -> {
                }
                case STRING -> {
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .type(Types.String)
                            .with(b -> b.builder().set("default", defaultValue))
                            .description("Default: " + defaultValue.asText())
                            .build());
                }
            }
        }
        schema.properties(properties.build());
        return schema.build();
    }
}
