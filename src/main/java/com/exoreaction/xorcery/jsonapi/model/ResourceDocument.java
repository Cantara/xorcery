package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.util.builders.With;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author rickardoberg
 */
public record ResourceDocument(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<ResourceDocument.Builder>
    {
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
        return new Errors(object().path("errors") instanceof ArrayNode array ? array :
                JsonNodeFactory.instance.arrayNode());
    }

    public Meta getMeta() {
        return new Meta(object().path("meta") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public JsonApi getJsonapi() {
        return new JsonApi(object().path("jsonapi") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Links getLinks() {
        return new Links(object().path("links") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public Included getIncluded() {
        return new Included(object().path("included") instanceof ArrayNode array ? array :
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
}
