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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.builders.With;
import dev.xorcery.json.JsonElement;

import java.util.Objects;

/**
 * @author rickardoberg
 */
public record ResourceObject(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {

        public Builder(String type, String id) {
            this(JsonNodeFactory.instance.objectNode());

            if (id != null)
                builder.set("id", builder.textNode(id));

            builder.set("type", builder.textNode(type));
        }

        public Builder(Enum<?> type, String id) {
            this(type.name(), id);
        }

        public Builder(String type) {
            this(JsonNodeFactory.instance.objectNode());
            builder.set("type", builder.textNode(type));
        }

        public Builder(ResourceObject resourceObject) {
            this(resourceObject.json().deepCopy());
        }

        public Builder attributes(Attributes attributes) {
            ObjectNode object = attributes.object();
            if (!object.isEmpty())
                builder.set("attributes", object);
            return this;
        }

        public Builder attributes(Attributes.Builder attributes) {
            return attributes(attributes.build());
        }

        public Builder relationships(Relationships relationships) {
            ObjectNode object = relationships.object();
            if (!object.isEmpty())
                builder.set("relationships", object);
            return this;
        }

        public Builder relationships(Relationships.Builder relationships) {
            return relationships(relationships.build());
        }

        public Builder links(Links links) {
            builder.set("links", links.json());
            return this;
        }

        public Builder links(Links.Builder links) {
            return links(links.build());
        }

        public Builder meta(Meta meta) {
            builder.set("meta", meta.json());
            return this;
        }

        public ResourceObject build() {
            return new ResourceObject(builder);
        }
    }

    public String getId() {
        return object().path("id").textValue();
    }

    public String getType() {
        return object().path("type").textValue();
    }

    public ResourceObjectIdentifier getResourceObjectIdentifier() {
        return new ResourceObjectIdentifier.Builder(getType(), getId()).build();
    }

    public Attributes getAttributes() {
        return new Attributes(object().path("attributes") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Relationships getRelationships() {
        return new Relationships(object().path("relationships") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Links getLinks() {
        return new Links(object().path("links") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Meta getMeta() {
        return new Meta(object().path("meta") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceObject that = (ResourceObject) o;
        return getType().equals(that.getType()) && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }
}
