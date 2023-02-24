package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * @author rickardoberg
 */
public final class ResourceObjects
        implements JsonElement, Iterable<ResourceObject> {
    private final ArrayNode json;

    /**
     *
     */
    public ResourceObjects(ArrayNode json) {
        this.json = json;
    }

    public static Collector<ResourceObject, Builder, ResourceObjects> toResourceObjects() {
        return Collector.of(Builder::new, (builder, ro) -> {
            if (ro != null) builder.resource(ro);
        }, (builder1, builder2) -> builder1, Builder::build);
    }

    public static ResourceObjects toResourceObjects(Stream<ResourceObject> stream) {
        return stream.collect(ResourceObjects.toResourceObjects());
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

            public Builder resource(ResourceObject resourceObject) {
                builder.add(resourceObject.json());
                return this;
            }

            public ResourceObjects build() {
                return new ResourceObjects(builder);
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

    public List<ResourceObject> getResources() {
        return JsonElement.getValuesAs(array(), ResourceObject::new);
    }

    @Override
    public Iterator<ResourceObject> iterator() {
        return getResources().iterator();
    }

    public Stream<ResourceObject> stream() {
        return getResources().stream();
    }

    public ResourceObjectIdentifiers getResourceObjectIdentifiers() {
        return new ResourceObjectIdentifiers.Builder().resources(this).build();
    }

    @Override
    public ArrayNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceObjects) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "ResourceObjects[" +
               "json=" + json + ']';
    }

}
