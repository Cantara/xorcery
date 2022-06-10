package com.exoreaction.reactiveservices.jsonapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.uri.UriTemplate;

import java.net.URI;
import java.util.Optional;

/**
 * @author rickardoberg
 */
public record Link(String rel, JsonNode value) {
    public Link(String rel, String uri) {
        this(rel, JsonNodeFactory.instance.textNode(uri));
    }

    public String getHref() {
        return value.isTextual() ? value.textValue() : value.path("href").textValue();
    }

    public URI getHrefAsUri() {
        return URI.create(getHref());
    }

    public UriBuilder getHrefAsUriBuilder() {
        return UriBuilder.fromUri(getHref());
    }

    public UriTemplate getHrefAsUriTemplate() {
        return new UriTemplate(getHref());
    }

    public boolean isTemplate() {
        return !new UriTemplate(getHref()).getTemplateVariables().isEmpty();
    }

    public Optional<Meta> getMeta() {
        if (value.isTextual()) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(value.path("meta")).map(ObjectNode.class::cast).map(Meta::new);
        }
    }
}
