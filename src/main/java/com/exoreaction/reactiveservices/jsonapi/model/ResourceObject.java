package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.Objects;

import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static java.util.Optional.ofNullable;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record ResourceObject(JsonObject json)
        implements JsonElement {
    public record Builder(JsonObjectBuilder builder) {

        public Builder(String type, String id) {
            this(Json.createObjectBuilder());
            builder.add("id", id)
                    .add("type", type);
        }

        public Builder(Enum<?> type, String id) {
            this(id, type.name());
        }

        public Builder(String type) {
            this(Json.createObjectBuilder());
            builder.add("type", type);
        }

        public Builder(ResourceObject resourceObject) {
            this(Json.createObjectBuilder(resourceObject.json()));
        }

        public Builder attributes(Attributes attributes) {
            JsonObject object = attributes.object();
            if (!object.isEmpty())
                builder.add("attributes", object);
            return this;
        }

        public Builder attributes(Attributes.Builder attributes) {
            return attributes(attributes.build());
        }

        public Builder relationships(Relationships relationships) {
            JsonObject object = relationships.object();
            if (!object.isEmpty())
                builder.add("relationships", object);
            return this;
        }

        public Builder relationships(Relationships.Builder relationships) {
            return relationships(relationships.build());
        }

        public Builder links(Links links) {
            builder.add("links", links.json());
            return this;
        }

        public Builder links(Links.Builder links) {
            return links(links.build());
        }

        public Builder meta(Meta meta) {
            builder.add("meta", meta.json());
            return this;
        }

        public ResourceObject build() {
            return new ResourceObject(builder.build());
        }
    }

    public String getId() {
        return object().getString("id", null);
    }

    public String getType() {
        return object().getString("type");
    }

    public ResourceObjectIdentifier getResourceObjectIdentifier() {
        return new ResourceObjectIdentifier.Builder(getType(), getId()).build();
    }

    public Attributes getAttributes() {
        return new Attributes(ofNullable(object().getJsonObject("attributes")).orElse(EMPTY_JSON_OBJECT));
    }

    public Relationships getRelationships() {
        return new Relationships(ofNullable(object().getJsonObject("relationships")).orElse(EMPTY_JSON_OBJECT));
    }

    public Links getLinks() {
        return new Links(ofNullable(object().getJsonObject("links")).orElse(EMPTY_JSON_OBJECT));
    }

    public Meta getMeta() {
        return new Meta(ofNullable(object().getJsonObject("meta")).orElse(EMPTY_JSON_OBJECT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceObject that = (ResourceObject) o;
        return getType().equals(that.getType()) && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }
}
