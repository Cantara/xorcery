package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 */
public record Relationship(ObjectNode json)
        implements JsonElement, Consumer<Relationship.Builder> {

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {

        public static Relationship relationship(ResourceObject resourceObject) {
            return new Builder().resourceIdentifier(resourceObject).build();
        }

        public static Relationship relationship(String type, String id) {
            return new Builder().resourceIdentifier(new ResourceObjectIdentifier.Builder(type, id).build()).build();
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder links(Links value) {
            builder.set("links", value.json());
            return this;
        }

        public Builder link(String rel, URI value) {
            return links(new Links.Builder().link(rel, value).build());
        }

        public Builder link(String rel, String value) {
            return links(new Links.Builder().link(rel, value).build());
        }

        public Builder meta(Meta value) {
            if (!value.getMeta().isEmpty()) {
                builder.set("meta", value.json());
            }
            return this;
        }

        public Builder resourceIdentifier(ResourceObjectIdentifier value) {
            builder.set("data", value == null ? NullNode.getInstance() : value.json());
            return this;
        }

        public Builder resourceIdentifiers(ResourceObjectIdentifiers value) {
            builder.set("data", value.json());
            return this;
        }

        public Builder resourceIdentifier(ResourceObject value) {
            builder.set("data", value == null ? NullNode.getInstance() : value.getResourceObjectIdentifier().json());
            return this;
        }

        public Builder resourceIdentifiers(ResourceObjects value) {
            builder.set("data", value.getResourceObjectIdentifiers().json());
            return this;
        }

        public Relationship build() {
            return new Relationship(builder);
        }
    }


    public boolean isIncluded() {
        return object().hasNonNull("data");
    }

    public boolean isMany() {
        return object().path("data").isArray();
    }

    public Meta getMeta() {
        return new Meta(object().path("meta") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Links getLinks() {
        return new Links(object().path("links") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Optional<ResourceObjectIdentifier> getResourceObjectIdentifier() {
        JsonNode data = object().get("data");
        if (data == null || data.isArray()) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObjectIdentifier((ObjectNode) data));
    }

    public Optional<ResourceObjectIdentifiers> getResourceObjectIdentifiers() {
        JsonNode data = object().get("data");
        if (data == null || data.isObject()) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObjectIdentifiers((ArrayNode) data));
    }

    @Override
    public void accept(Builder builder) {
        getResourceObjectIdentifier().ifPresent(builder::resourceIdentifier);
        getResourceObjectIdentifiers().ifPresent(builder::resourceIdentifiers);
    }

    public boolean contains(ResourceObjectIdentifier resourceObjectIdentifier) {
        return getResourceObjectIdentifier()
                .map(roi -> roi.equals(resourceObjectIdentifier))
                .orElseGet(() -> getResourceObjectIdentifiers()
                        .map(rois -> rois.contains(resourceObjectIdentifier))
                        .orElse(false));
    }

}
