package com.exoreaction.xorcery.hyperschema.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author rickardoberg
 */

public final class Links
        implements JsonElement {
    private final ArrayNode json;

    /**
     *
     */
    public Links(ArrayNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ArrayNode builder;

        public Builder(ArrayNode builder) {
            this.builder = builder;
        }

        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder link(Link value) {
            builder.add(value.json());
            return this;
        }

        public Links build() {
            return new Links(builder);
        }

        public ArrayNode builder() {
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

    public Optional<Link> getLinkByRel(String rel) {
        for (JsonNode jsonNode : json) {
            if (jsonNode.path("rel").textValue().equals(rel)) {
                return Optional.of(new Link((ObjectNode) jsonNode));
            }
        }
        return Optional.empty();
    }

    public Map<String, Link> getLinks() {
        Map<String, Link> links = new LinkedHashMap<>();
        array().forEach(
                (value) -> links.put(value.get("rel").textValue(), new Link((ObjectNode) value)));
        return links;
    }

    @Override
    public ArrayNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Links) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Links[" +
               "json=" + json + ']';
    }

}
