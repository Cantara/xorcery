package com.exoreaction.xorcery.hyperschema.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author rickardoberg
 * \
 */

public record Links(ArrayNode json)
        implements JsonElement {
    public record Builder(ArrayNode builder)
            implements With<Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder link(Link value) {
            builder.add(value.json());
            return this;
        }

        public Links build() {
            return new Links(builder);
        }
    }

    public Optional<Link> getLinkByRel(String rel) {
        for (JsonNode jsonNode : json) {
            if (jsonNode.path("rel").textValue().equals(rel)) {
                return Optional.of(new Link((ObjectNode) jsonNode));
            }
        }
        return Optional.empty();
    }

    public Map<String, Link> getLinks() {
        Map<String, Link> links = new LinkedHashMap<>();
        array().forEach(
                (value) -> links.put(value.get("rel").textValue(), new Link((ObjectNode) value)));
        return links;
    }
}
