package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 */

public final class Included
        implements JsonElement, Consumer<Included.Builder> {
    private final ArrayNode json;

    /**
     *
     */
    public Included(ArrayNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ArrayNode builder;
        private final Set<String> included;

        public Builder(ArrayNode builder, Set<String> included) {
            this.builder = builder;
            this.included = included;
        }

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
         *
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

        public ArrayNode builder() {
            return builder;
        }

        public Set<String> included() {
            return included;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder) &&
                   Objects.equals(this.included, that.included);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder, included);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ", " +
                   "included=" + included + ']';
        }

    }

    public List<ResourceObject> getIncluded() {
        return JsonElement.getValuesAs(array(), ResourceObject::new);
    }

    public Optional<ResourceObject> findByResourceObjectIdentifier(ResourceObjectIdentifier roi) {
        return getIncluded().stream().filter(ro -> ro.getId().equals(roi.getId()) && ro.getType().equals(roi.getType())).findFirst();
    }

    @Override
    public void accept(Builder builder) {
        getIncluded().forEach(builder::include);
    }

    @Override
    public ArrayNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Included) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Included[" +
               "json=" + json + ']';
    }

}
