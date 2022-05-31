package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
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
 * @since 01/12/2018
 */

public record Relationship(ObjectNode json)
        implements JsonElement, Consumer<Relationship.Builder> {

    public record Builder(ObjectNode builder) {

        public static Relationship relationship(ResourceObject resourceObject) {
            return new Builder().resourceIdentifier(resourceObject).build();
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

        public Builder with(Consumer<Builder> consumer) {
            consumer.accept(this);
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
        JsonNode data = object().path("data");
        if (data.isNull() || data.isArray()) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObjectIdentifier((ObjectNode) data));
    }

    public Optional<ResourceObjectIdentifiers> getResourceObjectIdentifiers() {
        JsonNode data = object().path("data");
        if (data.isNull() || data.isObject()) {
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
