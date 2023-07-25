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
package com.exoreaction.xorcery.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Optional;

/**
 * @author rickardoberg
 */
public record Link(String rel, JsonNode value) {
    public Link(String rel, String uri) {
        this(rel, JsonNodeFactory.instance.textNode(uri));
    }

    public Link(String rel, URI uri) {
        this(rel, JsonNodeFactory.instance.textNode(uri.toASCIIString()));
    }

    public String getHref() {
        return value.isTextual() ? value.textValue() : value.path("href").textValue();
    }

    public URI getHrefAsUri() {
        return URI.create(getHref());
    }

    public boolean isWebsocket() {
        return getHref().startsWith("ws") || getHref().startsWith("wss");
    }

    public Optional<Meta> getMeta() {
        if (value.isTextual()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(value.path("meta")).map(ObjectNode.class::cast).map(Meta::new);
        }
    }
}
