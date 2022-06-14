package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.builders.With;
import com.exoreaction.util.json.JsonNodes;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * @author rickardoberg
 */
public record ResourceObjects(ArrayNode json)
        implements JsonElement,Iterable<ResourceObject> {

    public static Collector<ResourceObject, Builder, ResourceObjects> toResourceObjects() {
        return Collector.of(Builder::new, (builder, ro) -> {
            if (ro != null) builder.resource(ro);
        }, (builder1, builder2) -> builder1, Builder::build);
    }

    public static ResourceObjects toResourceObjects(Stream<ResourceObject> stream)
    {
        return stream.collect(ResourceObjects.toResourceObjects());
    }

    public record Builder(ArrayNode builder)
            implements With<Builder>
    {
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
    }

    public List<ResourceObject> getResources() {
        return JsonNodes.getValuesAs(array(), ResourceObject::new);
    }

    @NotNull
    @Override
    public Iterator<ResourceObject> iterator() {
        return getResources().iterator();
    }

    public Stream<ResourceObject> stream()
    {
        return getResources().stream();
    }

    public ResourceObjectIdentifiers getResourceObjectIdentifiers() {
        return new ResourceObjectIdentifiers.Builder().resources(this).build();
    }
}
