package com.exoreaction.xorcery.jsonapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author rickardoberg
 */
public final class Link {
    private final String rel;
    private final JsonNode value;

    /**
     *
     */
    public Link(String rel, JsonNode value) {
        this.rel = rel;
        this.value = value;
    }

    public Link(String rel, String uri) {
        this(rel, JsonNodeFactory.instance.textNode(uri));
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

    public String rel() {
        return rel;
    }

    public JsonNode value() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Link) obj;
        return Objects.equals(this.rel, that.rel) &&
               Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rel, value);
    }

    @Override
    public String toString() {
        return "Link[" +
               "rel=" + rel + ", " +
               "value=" + value + ']';
    }

}
