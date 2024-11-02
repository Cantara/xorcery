/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.jsonapi;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.xorcery.builders.With;
import dev.xorcery.json.JsonElement;

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
        return JsonElement.getValuesAs(array(),ResourceObject::new);
    }

    public Optional<ResourceObject> findByResourceObjectIdentifier(ResourceObjectIdentifier roi) {
        return getIncluded().stream().filter(ro -> ro.getId().equals(roi.getId()) && ro.getType().equals(roi.getType())).findFirst();
    }

    @Override
    public void accept(Builder builder) {
        getIncluded().forEach(builder::include);
    }
}
