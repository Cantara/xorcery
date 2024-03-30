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
package com.exoreaction.xorcery.hyperschema;

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
 *
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
