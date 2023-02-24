package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;


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
            Iterator<String> names = object().fieldNames();
            while (names.hasNext()) {
                String rel = names.next();
                if (rel.contains(name))
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
