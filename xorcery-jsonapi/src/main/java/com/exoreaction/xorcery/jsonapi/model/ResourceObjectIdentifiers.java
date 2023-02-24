package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Objects;

/**
 * @author rickardoberg
 */
public final class ResourceObjectIdentifiers
        implements JsonElement {
    private final ArrayNode json;

    /**
     *
     */
    public ResourceObjectIdentifiers(ArrayNode json) {
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

        public Builder resource(ResourceObjectIdentifier resourceObjectIdentifier) {
            builder.add(resourceObjectIdentifier.json());
            return this;
        }

        public Builder resource(ResourceObject resourceObject) {
            builder.add(resourceObject.getResourceObjectIdentifier().json());
            return this;
        }

        public Builder resources(ResourceObjectIdentifiers resourceObjectIdentifiers) {
            resourceObjectIdentifiers.getResources().forEach(this::resource);
            return this;
        }

        public Builder resources(ResourceObjects resourceObjects) {
            for (ResourceObject resource : resourceObjects) {
                builder.add(resource.getResourceObjectIdentifier().json());
            }
            return this;
        }

        public ResourceObjectIdentifiers build() {
            return new ResourceObjectIdentifiers(builder);
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

    public List<ResourceObjectIdentifier> getResources() {
        return JsonElement.getValuesAs(array(), ResourceObjectIdentifier::new);
    }

    public boolean contains(ResourceObjectIdentifier resourceObjectIdentifier) {
        return getResources().stream().anyMatch(ro -> ro.equals(resourceObjectIdentifier));
    }

    public boolean contains(ResourceObject resourceObject) {
        return contains(resourceObject.getResourceObjectIdentifier());
    }

    @Override
    public ArrayNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceObjectIdentifiers) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "ResourceObjectIdentifiers[" +
               "json=" + json + ']';
    }

}
