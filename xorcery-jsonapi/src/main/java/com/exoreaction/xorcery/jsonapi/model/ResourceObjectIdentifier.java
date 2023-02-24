package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * @author rickardoberg
 */
public final class ResourceObjectIdentifier
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public ResourceObjectIdentifier(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

        public Builder(String type, String id) {
            this(JsonNodeFactory.instance.objectNode());
            builder.<ObjectNode>set("type", builder.textNode(type))
                    .set("id", builder.textNode(id));
        }

        public Builder(Enum<?> type, String id) {
            this(type.name(), id);
        }

        public Builder meta(Meta meta) {
            builder.set("meta", meta.json());
            return this;
        }

        public ResourceObjectIdentifier build() {
            return new ResourceObjectIdentifier(builder);
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

    public String getId() {
        return getString("id").orElse("");
    }

    public String getType() {
        return getString("type").orElseThrow();
    }

    public Meta getMeta() {
        JsonNode node = object().path("meta");
        return new Meta(node instanceof ObjectNode ? (ObjectNode) node :
                JsonNodeFactory.instance.objectNode());
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceObjectIdentifier) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "ResourceObjectIdentifier[" +
               "json=" + json + ']';
    }

}
