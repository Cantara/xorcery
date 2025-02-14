/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.jsonschema.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.jsonschema.Definitions;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.Properties;
import dev.xorcery.jsonschema.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This generator takes a best-effort approach to generate a JSON Schema based on
 * a JSON example document and optional resolved JSON example document (if using JsonResolver).
 * <p>
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

    public JsonSchema generateJsonSchema(ObjectNode example) {
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
                    definitions.definition(property.getKey(), new JsonSchema.Builder().type(Types.Array).build());
                    properties.property(property.getKey(), new JsonSchema.Builder().ref("#/$defs/" + property.getKey()).build());
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
                            .types(Types.String, Types.Null)
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
                    List<JsonSchema> itemSchemas = new ArrayList<>();
                    ArrayNode effectiveArray = (ArrayNode) effectiveValue;
                    for (JsonNode jsonNode : effectiveArray) {
                        if (jsonNode instanceof ObjectNode objectItem) {
                            itemSchemas.add(createSchema(rootSchema, objectItem, objectItem));
                        }
                    }
                    properties.property(property.getKey(), new JsonSchema.Builder()
                            .types(Types.Array, Types.String)
                            .with(builder -> {
                                if (!itemSchemas.isEmpty()) {
                                    builder.items(new JsonSchema.Builder().anyOf(itemSchemas).build());
                                }
                            })
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
