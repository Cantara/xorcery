package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record ResourceObjectIdentifier(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder) {

        public Builder(String type, String id) {
            this(JsonNodeFactory.instance.objectNode());
            builder.<ObjectNode>set("type", builder.textNode(type))
                    .set("id", builder.textNode(id));
        }

        public Builder(Enum<?> type, String id) {
            this(type.name(), id);
        }

        public Builder meta(Meta meta) {
            builder.set("meta", meta.json());
            return this;
        }

        public ResourceObjectIdentifier build() {
            return new ResourceObjectIdentifier(builder);
        }
    }

    public String getId() {
        return getString("id");
    }

    public String getType() {
        return getString("type");
    }

    public Meta getMeta() {
        return new Meta(object().path("meta") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }
}
