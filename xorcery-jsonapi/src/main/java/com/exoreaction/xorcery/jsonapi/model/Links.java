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
public final class Links
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public Links(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

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

        public ObjectNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
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

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Links) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Links[" +
               "json=" + json + ']';
    }

}
