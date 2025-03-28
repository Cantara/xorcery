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
package dev.xorcery.hyperschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.json.JsonElement;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.MediaTypes;
import dev.xorcery.jsonschema.Properties;
import dev.xorcery.jsonschema.Types;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.xorcery.lang.Strings.capitalize;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;


/**
 * @author rickardoberg
 */

public record Link(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder) {

        public static Link link(String title, String description, String rel, String targetMediaType) {
            return new Link.Builder()
                    .title(title)
                    .description(description)
                    .rel(rel)
                    .href(format("{+%s_href}", rel))
                    .templateRequired(format("%s_href", rel))
                    .templatePointer(format("%s_href", rel), format("/links/%s", rel))
                    .targetMediaType(targetMediaType)
                    .build();
        }

        public static Link link(String title, String description, String rel, String targetMediaType, String href) {
            return new Link.Builder()
                    .title(title)
                    .description(description)
                    .rel(rel)
                    .href(href)
                    .targetMediaType(targetMediaType)
                    .build();
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder href(String value) {
            builder.set("href", builder.textNode(value));
            return this;
        }

        public Builder rel(String value) {
            builder.set("rel", builder.textNode(value));
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

        public Builder templateRequired(String... values) {
            builder.set("templateRequired", JsonElement.toArray(values));
            return this;
        }

        public Builder templatePointer(String name, String value) {
            ObjectNode templatePointers = Optional.ofNullable((ObjectNode) builder.get("templatePointers"))
                    .orElseGet(() ->
                    {
                        ObjectNode value1 = builder.objectNode();
                        builder.set("templatePointers", value1);
                        return value1;
                    });

            templatePointers.set(name, builder.textNode(value));
            return this;
        }

        public Builder targetHint(String name, JsonNode value) {

            ObjectNode targetHints = Optional.ofNullable((ObjectNode) builder.get("targetHints"))
                    .orElseGet(() ->
                    {
                        ObjectNode value1 = builder.objectNode();
                        builder.set("targetHints", value1);
                        return value1;
                    });
            targetHints.set(name, value);
            return this;
        }

        public Builder targetMediaType(String value) {
            builder.set("targetMediaTypes", builder.textNode(value));
            return this;
        }

        public Builder targetSchema(JsonSchema value) {
            builder.set("targetSchema", value.json());
            return this;
        }

        public Builder hrefSchema(JsonSchema value) {
            builder.set("hrefSchema", value.json());
            return this;
        }

        public Builder submissionMediaType(String value) {
            builder.set("submissionMediaType", builder.textNode(value));
            return this;
        }

        public Builder submissionSchema(JsonSchema value) {
            builder.set("submissionSchema", value.json());
            return this;
        }

        public Link build() {
            return new Link(builder);
        }
    }

    public record UriTemplateBuilder(String rel, Link.Builder builder, Properties.Builder properties) {
        public UriTemplateBuilder(String rel) {
            this(rel, new Link.Builder(), new Properties.Builder());
            builder.rel(rel)
                    .href("{+" + rel + "_href}")
                    .templateRequired(rel + "_href")
                    .templatePointer(rel + "href", "/links/" + rel)
                    .targetMediaType(MediaTypes.APPLICATION_JSON_API);
        }

        public UriTemplateBuilder parameter(String name, String title, String description) {
            properties.property(name, new JsonSchema.Builder()
                    .title(title)
                    .description(description)
                    .type(Types.String)
                    .build());

            return this;
        }

        public Link build() {
            return builder.hrefSchema(new JsonSchema.Builder()
                            .properties(properties.build())
                            .build())
                    .build();
        }
    }

    public Optional<String> getHref() {
        return getString("href");
    }

    public Optional<String> getRel() {
        return getString("rel");
    }

    public Optional<String> getTitle() {
        return getString("title");
    }

    public Optional<String> getDescription() {
        return getString("description");
    }

    public Optional<JsonSchema> getHrefSchema() {
        return ofNullable(object().get("hrefSchema"))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Optional<JsonSchema> getTargetSchema() {
        return ofNullable(object().get("targetSchema"))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Optional<JsonSchema> getSubmissionSchema() {
        return ofNullable(object().get("submissionSchema"))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Optional<Map<String, String>> getTargetPointers() {
        return ofNullable(object().get("targetPointers"))
                .map(ObjectNode.class::cast)
                .map(v -> JsonElement.toMap(v, JsonNode::textValue));
    }

    public Optional<ObjectNode> getTargetHints() {
        return ofNullable(object().get("targetHints")).map(ObjectNode.class::cast);
    }

    public boolean isDeleteAllowed() {
        return getTargetHints()
                .map(js -> js.get("allow"))
                .map(ArrayNode.class::cast)
                .map(allowed ->
                {
                    for (int i = 0; i < allowed.size(); i++) {
                        JsonNode v = allowed.get(i);
                        if (v.textValue().equals("DELETE"))
                            return true;
                    }
                    return false;
                }).orElse(false);
    }

    public Optional<String> getSubmissionMediaType() {
        return getString("submissionMediaType");
    }

    public Optional<Types> type() {
        return getString("type")
                .map((type) -> Types.valueOf(capitalize(type)));
    }

    public Optional<List<String>> getTemplateRequired() {
        return ofNullable(object().get("templateRequired"))
                .map(ArrayNode.class::cast)
                .map(a -> JsonElement.getValuesAs(a, JsonNode::textValue));
    }
}
