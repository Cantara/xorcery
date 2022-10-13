package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 */
public record ResourceObjectIdentifier(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {

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
        return getString("id").orElse("");
    }

    public String getType() {
        return getString("type").orElseThrow();
    }

    public Meta getMeta() {
        return new Meta(object().path("meta") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }
}
