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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.builders.With;
import dev.xorcery.json.JsonElement;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;


/**
 * @author rickardoberg
 */
public record Links(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder link(Consumer<Builder> consumer) {
            consumer.accept(this);
            return this;
        }

        public Builder link(String rel, String href) {
            builder.set(rel, builder.textNode(href));
            return this;
        }

        public Builder link(String rel, URI href) {
            return link(rel, href.toASCIIString());
        }

        public Builder link(Enum<?> rel, URI href) {
            return link(rel.name(), href);
        }

        public Builder link(String rel, String href, Meta meta) {
            builder.set(rel, builder.objectNode()
                    .<ObjectNode>set("href", builder.textNode(href))
                    .set("meta", meta.json()));
            return this;
        }

        public Builder link(String rel, URI href, Meta meta) {
            return link(rel, href.toASCIIString(), meta);
        }

        public Links build() {
            return new Links(builder);
        }
    }

    private static final Pattern spaceSeparated = Pattern.compile(" ");

    public boolean isEmpty() {
        return object().isEmpty();
    }

    public Optional<Link> getSelf() {
        return getByRel("self");
    }

    public Optional<Link> getByRel(String name) {
        if (name == null) {
            return Optional.empty();
        } else {
            String[] findRels = spaceSeparated.split(name);
            Iterator<String> names = object().fieldNames();
            nextRel: while (names.hasNext()) {
                String rel = names.next();
                String[] rels = spaceSeparated.split(rel);
                for (String findRel : findRels) {
                    if (!Arrays.asList(rels).contains(findRel))
                        continue nextRel;
                }
                return Optional.of(new Link(rel, object().get(rel)));
            }
            return Optional.empty();
        }
    }

    public List<Link> getLinks() {

        Iterator<Map.Entry<String, JsonNode>> fields = object().fields();
        List<Link> links = new ArrayList<>(object().size());
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            links.add(new Link(next.getKey(), next.getValue()));
        }
        return links;
    }
}
