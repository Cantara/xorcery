package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author rickardoberg
 */
public final class ResourceDocument
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public ResourceDocument(ObjectNode json) {
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

        public Builder data(ResourceObject.Builder value) {
            return data(value.build());
        }

        public Builder data(ResourceObject value) {
            if (value == null) {
                builder.putNull("data");
            } else {
                builder.set("data", value.json());
            }
            return this;
        }

        public Builder data(ResourceObjects.Builder value) {
            return data(value.build());
        }

        public Builder data(ResourceObjects value) {
            builder.set("data", value.json());
            return this;
        }

        public Builder errors(Errors.Builder value) {
            return errors(value.build());
        }

        public Builder errors(Errors value) {
            builder.set("errors", value.json());
            return this;
        }

        public Builder meta(Meta value) {
            if (!value.object().isEmpty()) {
                builder.set("meta", value.json());
            }
            return this;
        }

        public Builder jsonapi(JsonApi value) {
            if (!value.object().isEmpty()) {
                builder.set("jsonapi", value.json());
            }
            return this;
        }

        public Builder links(Links.Builder value) {
            return links(value.build());
        }

        public Builder links(Links value) {
            if (!value.object().isEmpty()) {
                builder.set("links", value.json());
            }
            return this;
        }

        public Builder included(Included.Builder value) {
            return included(value.build());
        }

        public Builder included(Included value) {
            if (!value.array().isEmpty()) {
                builder.set("included", value.json());
            }
            return this;
        }

        public ResourceDocument build() {
            return new ResourceDocument(builder);
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

    public boolean isCollection() {
        return object().get("data") instanceof ArrayNode;
    }

    public Optional<ResourceObject> getResource() {
        JsonNode data = object().path("data");
        if (!data.isObject()) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObject((ObjectNode) data));
    }


    public Optional<ResourceObjects> getResources() {
        JsonNode data = object().path("data");
        if (!data.isArray()) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObjects(((ArrayNode) data)));
    }

    public Errors getErrors() {
        JsonNode node = object().path("errors");
        return new Errors(node instanceof ArrayNode ? (ArrayNode) node :
                JsonNodeFactory.instance.arrayNode());
    }

    public Meta getMeta() {
        JsonNode node = object().path("meta");
        return new Meta(node instanceof ObjectNode ? (ObjectNode) node :
                JsonNodeFactory.instance.objectNode());
    }

    public JsonApi getJsonapi() {
        JsonNode node = object().path("jsonapi");
        return new JsonApi(node instanceof ObjectNode ? (ObjectNode) node :
                JsonNodeFactory.instance.objectNode());
    }

    public Links getLinks() {
        JsonNode node = object().path("links");
        return new Links(node instanceof ObjectNode ? (ObjectNode) node :
                JsonNodeFactory.instance.objectNode());
    }

    public Included getIncluded() {
        JsonNode node = object().path("included");
        return new Included(node instanceof ArrayNode ? (ArrayNode) node :
                JsonNodeFactory.instance.arrayNode());
    }

    /**
     * If this ResourceDocument is a collection of ResourceObjects, then split into a set of ResourceDocuments that are
     * single-resource. Referenced included ResourceObjects are included in the split documents, and duplicated if necessary.
     * <p>
     * If this ResourceDocument is a single ResourceObject, then just return it.
     *
     * @return
     */
    public Stream<ResourceDocument> split() {
        return getResources().map(ros -> ros.stream().map(ro ->
                        new Builder()
                                .data(ro)
                                .links(getLinks())
                                .meta(getMeta())
                                .jsonapi(getJsonapi())
                                .included(new Included.Builder()
                                        .includeRelated(ro, getIncluded().getIncluded())
                                        .build())
                                .build()))
                .orElse(Stream.of(this));
    }

    /**
     * Return copy of ResourceDocument, but with all links having their host and port replaced
     *
     * @param baseUri URI with host and port to use
     * @return
     */
    public ResourceDocument resolve(URI baseUri) {
        ResourceDocument resolved = new ResourceDocument(json.deepCopy());

        resolve(baseUri, resolved.getLinks());
        resolved.getResource().ifPresent(ro -> resolve(baseUri, ro.getLinks()));
        resolved.getResources().ifPresent(ros -> ros.forEach(ro -> resolve(baseUri, ro.getLinks())));
        resolved.getIncluded().getIncluded().forEach(ro -> resolve(baseUri, ro.getLinks()));

        return resolved;
    }

    private void resolve(URI baseUri, Links links) {
        ObjectNode linksJson = links.json();
        Iterator<Map.Entry<String, JsonNode>> fields = linksJson.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            JsonNode node = next.getValue();
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                URI uri = URI.create(textNode.textValue());
                String resolvedUri;
                try {
                    resolvedUri = new URI(
                            baseUri.getScheme(),
                            uri.getUserInfo(),
                            baseUri.getHost(),
                            baseUri.getPort(),
                            uri.getPath(),
                            uri.getQuery(),
                            uri.getFragment()
                    ).toString();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                linksJson.set(next.getKey(), linksJson.textNode(resolvedUri));
            } else if (node instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) node;
                URI uri = URI.create(objectNode.get("href").textValue());
                String resolvedUri;
                try {
                    resolvedUri = new URI(
                            baseUri.getScheme(),
                            uri.getUserInfo(),
                            baseUri.getHost(),
                            baseUri.getPort(),
                            uri.getPath(),
                            uri.getQuery(),
                            uri.getFragment()
                    ).toString();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                objectNode.set(next.getKey(), linksJson.textNode(resolvedUri));
            }
        }
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceDocument) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "ResourceDocument[" +
               "json=" + json + ']';
    }

}
