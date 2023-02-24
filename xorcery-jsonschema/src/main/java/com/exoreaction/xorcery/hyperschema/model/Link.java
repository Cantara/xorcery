package com.exoreaction.xorcery.hyperschema.model;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.jsonschema.jaxrs.MediaTypes;
import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.exoreaction.xorcery.jsonschema.model.Properties;
import com.exoreaction.xorcery.jsonschema.model.Types;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.exoreaction.xorcery.util.Strings.capitalize;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;


/**
 * @author rickardoberg
 */

public final class Link
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public Link(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

        public static Link link(String title, String description, String rel, String targetMediaType) {
            return new Builder()
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
            return new Builder()
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
            ObjectNode templatePointers = ofNullable((ObjectNode) builder.get("templatePointers"))
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

            ObjectNode targetHints = ofNullable((ObjectNode) builder.get("targetHints"))
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

        public ObjectNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
        }

    }

    public static final class UriTemplateBuilder {
        private final String rel;
        private final Builder builder;
        private final Properties.Builder properties;

        public UriTemplateBuilder(String rel, Builder builder, Properties.Builder properties) {
            this.rel = rel;
            this.builder = builder;
            this.properties = properties;
        }

        public UriTemplateBuilder(String rel) {
            this(rel, new Builder(), new Properties.Builder());
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

        public String rel() {
            return rel;
        }

        public Builder builder() {
            return builder;
        }

        public Properties.Builder properties() {
            return properties;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (UriTemplateBuilder) obj;
            return Objects.equals(this.rel, that.rel) &&
                   Objects.equals(this.builder, that.builder) &&
                   Objects.equals(this.properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rel, builder, properties);
        }

        @Override
        public String toString() {
            return "UriTemplateBuilder[" +
                   "rel=" + rel + ", " +
                   "builder=" + builder + ", " +
                   "properties=" + properties + ']';
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

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Link) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Link[" +
               "json=" + json + ']';
    }

}
