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

import java.util.List;

/**
 * @author rickardoberg
 */
public record ResourceObjectIdentifiers(ArrayNode json)
        implements JsonElement {

    public static Builder newResourceObjectIdentifiers(){
        return new Builder();
    }

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
        return JsonElement.getValuesAs(array(), ResourceObjectIdentifier::new);
    }

    public boolean contains(ResourceObjectIdentifier resourceObjectIdentifier) {
        return getResources().stream().anyMatch(ro -> ro.equals(resourceObjectIdentifier));
    }

    public boolean contains(ResourceObject resourceObject) {
        return contains(resourceObject.getResourceObjectIdentifier());
    }
}
