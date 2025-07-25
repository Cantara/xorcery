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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.builders.With;
import dev.xorcery.json.JsonElement;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 */
public record Relationship(ObjectNode json)
        implements JsonElement, Consumer<Relationship.Builder> {

    public static Builder newRelationship(){
        return new Builder();
    }

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {

        public static Relationship relationship(ResourceObject resourceObject) {
            return new Builder().resourceIdentifier(resourceObject).build();
        }

        public static Relationship relationship(String type, String id) {
            return new Builder().resourceIdentifier(new ResourceObjectIdentifier.Builder(type, id).build()).build();
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder links(Links value) {
            builder.set("links", value.json());
            return this;
        }

        public Builder link(String rel, URI value) {
            return links(new Links.Builder().link(rel, value).build());
        }

        public Builder link(String rel, String value) {
            return links(new Links.Builder().link(rel, value).build());
        }

        public Builder meta(Meta value) {
            if (!value.getMeta().isEmpty()) {
                builder.set("meta", value.json());
            }
            return this;
        }

        public Builder resourceIdentifier(ResourceObjectIdentifier value) {
            builder.set("data", value == null ? NullNode.getInstance() : value.json());
            return this;
        }

        public Builder resourceIdentifiers(ResourceObjectIdentifiers value) {
            builder.set("data", value.json());
            return this;
        }

        public Builder resourceIdentifier(ResourceObject value) {
            builder.set("data", value == null ? NullNode.getInstance() : value.getResourceObjectIdentifier().json());
            return this;
        }

        public Builder resourceIdentifiers(ResourceObjects value) {
            builder.set("data", value.getResourceObjectIdentifiers().json());
            return this;
        }

        public Relationship build() {
            return new Relationship(builder);
        }
    }


    public boolean isIncluded() {
        return object().hasNonNull("data");
    }

    public boolean isMany() {
        return object().path("data").isArray();
    }

    public Meta getMeta() {
        return new Meta(object().path("meta") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Links getLinks() {
        return new Links(object().path("links") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Optional<ResourceObjectIdentifier> getResourceObjectIdentifier() {
        JsonNode data = object().get("data");
        if (data == null || data.isArray()) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObjectIdentifier((ObjectNode) data));
    }

    public Optional<ResourceObjectIdentifiers> getResourceObjectIdentifiers() {
        JsonNode data = object().get("data");
        if (data == null || data.isObject()) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObjectIdentifiers((ArrayNode) data));
    }

    @Override
    public void accept(Builder builder) {
        getResourceObjectIdentifier().ifPresent(builder::resourceIdentifier);
        getResourceObjectIdentifiers().ifPresent(builder::resourceIdentifiers);
    }

    public boolean contains(ResourceObjectIdentifier resourceObjectIdentifier) {
        return getResourceObjectIdentifier()
                .map(roi -> roi.equals(resourceObjectIdentifier))
                .orElseGet(() -> getResourceObjectIdentifiers()
                        .map(rois -> rois.contains(resourceObjectIdentifier))
                        .orElse(false));
    }

}
