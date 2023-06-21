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
package com.exoreaction.xorcery.jsonschema;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.lang.Strings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        return getString("$id");
    }
    public Optional<String> getRef() {
        return getString("$ref");
    }

    public Optional<String> getSchema() {
        return getString("$schema");
    }

    public Optional<Vocabularies> getVocabularies()
    {
        return Optional.ofNullable(json.get("$vocabulary")).map(ObjectNode.class::cast).map(Vocabularies::new);
    }

    public Optional<String> getTitle() {
        return getString("title");
    }

    public Optional<String> getDescription() {
        return getString("description");
    }

    public Types getType() {
        return getString("type").map(t -> Types.valueOf(Strings.capitalize(t))).orElse(Types.Null);
    }

    public Optional<List<String>> getRequired() {
        return Optional.ofNullable(object().get("required"))
                .map(ArrayNode.class::cast)
                .map(a -> JsonElement.getValuesAs(a, JsonNode::textValue));
    }

    public Optional<String> getConstant() {
        return getString("const");
    }

    public Optional<Boolean> getAdditionalProperties() {
        return getBoolean("additionalProperties");
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
