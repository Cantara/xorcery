package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.*;
import java.util.function.Consumer;


/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record Links(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder) {
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

        public Builder link(String rel, UriBuilder href) {
            return link(rel, href.build());
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

        public Builder link(String rel, UriBuilder href, Meta meta) {
            return link(rel, href.build().toASCIIString(), meta);
        }

        @SafeVarargs
        public final Builder with(Consumer<Builder>... consumers) {
            for (Consumer<Builder> consumer : consumers) {
                consumer.accept(this);
            }
            return this;
        }

        public Links build() {
            return new Links(builder);
        }
    }

    public Optional<Link> getSelf() {
        return getByRel("self");
    }

    public Optional<Link> getByRel(String name) {
        return Optional.ofNullable(object().get(name)).map(v -> new Link(name, v));
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
