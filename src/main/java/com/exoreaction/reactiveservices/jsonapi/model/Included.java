package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.JsonNodes;
import com.exoreaction.util.With;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 */

public record Included(ArrayNode json)
        implements JsonElement, Consumer<Included.Builder> {

    public record Builder(ArrayNode builder, Set<String> included)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode(), new HashSet<>());
        }

        public Builder include(ResourceObject resourceObject) {
            String resourceId = resourceObject.getId() + ":" + resourceObject.getType();
            if (!included.contains(resourceId)) {
                builder.add(resourceObject.json());
                included.add(resourceId);
            }
            return this;
        }

        public ResourceObject add(ResourceObject resourceObject) {
            include(resourceObject);
            return resourceObject;
        }

        public ResourceObjects add(ResourceObjects resourceObjects) {
            include(resourceObjects);
            return resourceObjects;
        }

        public Builder include(ResourceObjects resourceObjects) {
            for (ResourceObject resource : resourceObjects) {
                include(resource);
            }
            return this;
        }

        /**
         * For each relationship of resourceObject include all ResourceObjects from included, recursively
         * @param resourceObject
         * @param included
         * @return
         */
        public Builder includeRelated(ResourceObject resourceObject, List<ResourceObject> included) {
            for (Relationship relationship : resourceObject.getRelationships().getRelationshipList()) {
                relationship.getResourceObjectIdentifier().ifPresentOrElse(roi ->
                {
                    included.stream().filter(iro -> iro.getResourceObjectIdentifier().equals(roi))
                            .findFirst().ifPresent(iro ->
                            {
                                add(iro);
                                includeRelated(iro, included);
                            });
                }, () ->
                {
                    relationship.getResourceObjectIdentifiers()
                            .map(rois -> included.stream().filter(rois::contains))
                            .ifPresent(iros ->
                            {
                                iros.forEach(iro ->
                                {
                                    add(iro);
                                    includeRelated(iro, included);
                                });
                            });
                });
            }
            return this;
        }

        public Builder with(Consumer<Builder> consumer) {
            consumer.accept(this);
            return this;
        }

        public Included build() {
            return new Included(builder);
        }

        public boolean hasIncluded(String id, String type) {
            return included.contains(id + ":" + type);
        }

        public boolean hasIncluded(String id, Enum<?> type) {
            return included.contains(id + ":" + type);
        }
    }

    public List<ResourceObject> getIncluded() {
        return JsonNodes.getValuesAs(array(),ResourceObject::new);
    }

    public Optional<ResourceObject> findByResourceObjectIdentifier(ResourceObjectIdentifier roi) {
        return getIncluded().stream().filter(ro -> ro.getId().equals(roi.getId()) && ro.getType().equals(roi.getType())).findFirst();
    }

    @Override
    public void accept(Builder builder) {
        getIncluded().forEach(builder::include);
    }
}
