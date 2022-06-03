package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.JsonNodes;
import com.exoreaction.util.With;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.List;

/**
 * @author rickardoberg
 * @since 28/11/2018
 */

public record ResourceObjectIdentifiers(ArrayNode json)
        implements JsonElement {
    public record Builder(ArrayNode builder)
            implements With<Builder>
    {
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
    }

    public List<ResourceObjectIdentifier> getResources() {
        return JsonNodes.getValuesAs(array(), ResourceObjectIdentifier::new);
    }

    public boolean contains(ResourceObjectIdentifier resourceObjectIdentifier) {
        return getResources().stream().anyMatch(ro -> ro.equals(resourceObjectIdentifier));
    }

    public boolean contains(ResourceObject resourceObject) {
        return contains(resourceObject.getResourceObjectIdentifier());
    }
}
