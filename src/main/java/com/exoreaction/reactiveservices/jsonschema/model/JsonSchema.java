package com.exoreaction.reactiveservices.jsonschema.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * @author rickardoberg
 */
public record JsonSchema(ObjectNode json)
        implements JsonElement {
    public static final String DRAFT_7 = "http://json-schema.org/draft-07/schema#";
    public static final String HYPER_SCHEMA_DRAFT_7 = "http://json-schema.org/draft-07/hyper-schema#";

    public record Builder(ObjectNode builder) {

        public Builder()
        {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder id(String value) {
            builder.set("$id", builder.textNode(value));
            return this;
        }

        public Builder ref(String value) {
            builder.set("$ref", builder.textNode(value));
            return this;
        }

        public Builder schema(String versionUrl) {
            builder.set("$schema", builder.textNode(versionUrl));
            return this;
        }

        public Builder vocabularies(Vocabularies vocabularies) {
            builder.set("$vocabulary", vocabularies.json());
            return this;
        }

        public Builder title(String value) {
            builder.set("title", builder.textNode(value));
            return this;
        }

        public Builder description(String value) {
            builder.set("description", builder.textNode(value));
            return this;
        }

        public Builder allOf(JsonSchema... schemas) {
            builder.set("allOf", JsonElement.toArray(schemas));
            return this;
        }

        public Builder anyOf(JsonSchema... schemas) {
            builder.set("anyOf", JsonElement.toArray(schemas));
            return this;
        }

        public Builder oneOf(JsonSchema... schemas) {
            builder.set("oneOf", JsonElement.toArray(schemas));
            return this;
        }

        public Builder not(JsonSchema schema) {
            builder.set("not", schema.json());
            return this;
        }

        public Builder type(Types value) {
            builder.set("type", builder.textNode(value.name().toLowerCase()));
            return this;
        }

        // Objects
        public Builder required(String... values) {
            builder.set("required", JsonElement.toArray(values));
            return this;
        }

        // Strings
        public Builder enums(String... values) {
            builder.set("enum", JsonElement.toArray(values));
            return this;
        }

        public Builder constant(JsonNode value) {
            builder.set("const", value);
            return this;
        }

        // Arrays
        public Builder items(JsonSchema value) {
            builder.set("items", value.json());
            return this;
        }

        public Builder items(JsonSchema... values) {
            builder.set("items", JsonElement.toArray(values));
            return this;
        }

        public Builder items(Collection<JsonSchema> values) {
            if (values.size() == 1) {
                return items(values.iterator().next());
            }

            builder.set("items", JsonElement.toArray(values));
            return this;
        }

        public Builder additionalProperties(boolean value) {
            builder.set("additionalProperties", builder.booleanNode(value));
            return this;
        }

        public Builder definitions(Definitions value) {
            builder.set("definitions", value.json());
            return this;
        }

        public Builder properties(Properties value) {
            builder.set("properties", value.json());
            return this;
        }

        public JsonSchema build() {
            return new JsonSchema(builder);
        }
    }

    public Optional<String> getId() {
        return getOptionalString("$id");
    }
    public Optional<String> getRef() {
        return getOptionalString("$ref");
    }

    public Optional<String> getSchema() {
        return getOptionalString("$schema");
    }

    public Optional<Vocabularies> getVocabularies()
    {
        return Optional.ofNullable(json.get("$vocabulary")).map(ObjectNode.class::cast).map(Vocabularies::new);
    }

    public Optional<String> getTitle() {
        return getOptionalString("title");
    }

    public Optional<String> getDescription() {
        return getOptionalString("description");
    }

    public Types getType() {
        String type = getString("type");

        return Types.valueOf(capitalize(type));
    }

    public Optional<List<String>> getRequired() {
        return Optional.ofNullable(object().get("required"))
                .map(ArrayNode.class::cast)
                .map(a -> JsonNodes.getValuesAs(a, JsonNode::textValue));
    }

    public Optional<String> getConstant() {
        return getOptionalString("const");
    }

    public Optional<Boolean> getAdditionalProperties() {
        return getOptionalBoolean("additionalProperties");
    }

    public Definitions getDefinitions() {
        return Optional.ofNullable(object().get("definitions"))
                .map(ObjectNode.class::cast)
                .map(Definitions::new)
                .orElseGet(() -> new Definitions(json.objectNode()));
    }

    public Properties getProperties() {
        return Optional.ofNullable(object().get("properties"))
                .map(ObjectNode.class::cast)
                .map(Properties::new)
                .orElseGet(() -> new Properties(json.objectNode()));
    }
}
