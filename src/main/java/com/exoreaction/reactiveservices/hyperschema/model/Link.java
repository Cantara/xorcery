package com.exoreaction.reactiveservices.hyperschema.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.reactiveservices.jsonschema.model.JsonSchema;
import com.exoreaction.reactiveservices.jsonschema.model.Properties;
import com.exoreaction.reactiveservices.jsonschema.model.Types;
import com.exoreaction.util.json.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.HttpMethod;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.APPLICATION_JSON_API;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.capitalize;


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
                    .targetMediaType(APPLICATION_JSON_API);
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
        return getOptionalString("href");
    }

    public Optional<String> getRel() {
        return getOptionalString("rel");
    }

    public Optional<String> getTitle() {
        return getOptionalString("title");
    }

    public Optional<String> getDescription() {
        return getOptionalString("description");
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
                .map(v -> JsonNodes.toMap(v, JsonNode::textValue));
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
                        if (v.textValue().equals(HttpMethod.DELETE))
                            return true;
                    }
                    return false;
                }).orElse(false);
    }

    public Optional<String> getSubmissionMediaType() {
        return getOptionalString("submissionMediaType");
    }

    public Optional<Types> type() {
        return getOptionalString("type")
                .map((type) -> Types.valueOf(capitalize(type)));
    }

    public Optional<List<String>> getTemplateRequired() {
        return ofNullable(object().get("templateRequired"))
                .map(ArrayNode.class::cast)
                .map(a -> JsonNodes.getValuesAs(a, JsonNode::textValue));
    }
}
