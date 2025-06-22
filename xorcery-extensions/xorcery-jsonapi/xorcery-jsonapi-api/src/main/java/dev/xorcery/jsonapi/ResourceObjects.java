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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * @author rickardoberg
 */
public record ResourceObjects(ArrayNode json)
        implements JsonElement,Iterable<ResourceObject> {

    public static Builder newResourceObjects(){
        return new Builder();
    }

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
        return JsonElement.getValuesAs(array(), ResourceObject::new);
    }

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
