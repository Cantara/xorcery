package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.*;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record ResourceDocument(JsonObject json)
        implements JsonElement {

    public record Builder(JsonObjectBuilder builder) {
        public Builder() {
            this(Json.createObjectBuilder());
        }

        public Builder data(ResourceObject.Builder value) {
            return data(value.build());
        }

        public Builder data(ResourceObject value) {
            if (value == null) {
                builder.add("data", JsonValue.NULL);
            } else {
                builder.add("data", value.json());
            }
            return this;
        }

        public Builder data(ResourceObjects.Builder value) {
            return data(value.build());
        }

        public Builder data(ResourceObjects value) {
            builder.add("data", value.json());
            return this;
        }

        public Builder errors(Errors.Builder value) {
            return errors(value.build());
        }

        public Builder errors(Errors value) {
            builder.add("errors", value.json());
            return this;
        }

        public Builder meta(Meta value) {
            if (!value.object().isEmpty()) {
                builder.add("meta", value.json());
            }
            return this;
        }

        public Builder jsonapi(JsonApi value) {
            if (!value.object().isEmpty()) {
                builder.add("jsonapi", value.json());
            }
            return this;
        }

        public Builder links(Links.Builder value) {
            return links(value.build());
        }

        public Builder links(Links value) {
            if (!value.object().isEmpty()) {
                builder.add("links", value.json());
            }
            return this;
        }

        public Builder included(Included.Builder value) {
            return included(value.build());
        }

        public Builder included(Included value) {
            if (!value.array().isEmpty()) {
                builder.add("included", value.json());
            }
            return this;
        }

        public ResourceDocument build() {
            return new ResourceDocument(builder.build());
        }

    }

    public boolean isCollection() {
        return object().get("data") instanceof JsonArray;
    }

    public Optional<ResourceObject> getResource() {
        JsonValue data = object().get("data");
        if (data == null || data == JsonValue.NULL || data instanceof JsonArray) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObject((JsonObject) data));
    }


    public Optional<ResourceObjects> getResources() {
        JsonValue data = object().get("data");
        if (data == null || data instanceof JsonObject) {
            return Optional.empty();
        }

        return Optional.of(new ResourceObjects(data.asJsonArray()));
    }

    public Errors getErrors() {
        return new Errors(
                Optional.ofNullable(object().getJsonArray("errors")).orElse(JsonValue.EMPTY_JSON_ARRAY));
    }

    public Meta getMeta() {
        return new Meta(
                Optional.ofNullable(object().getJsonObject("meta")).orElse(JsonValue.EMPTY_JSON_OBJECT));
    }

    public JsonApi getJsonapi() {
        return new JsonApi(
                Optional.ofNullable(object().getJsonObject("jsonapi")).orElse(JsonValue.EMPTY_JSON_OBJECT));
    }

    public Links getLinks() {
        return new Links(
                Optional.ofNullable(object().getJsonObject("links")).orElse(JsonValue.EMPTY_JSON_OBJECT));
    }

    public Included getIncluded() {
        return new Included(
                Optional.ofNullable(object().getJsonArray("included")).orElse(JsonValue.EMPTY_JSON_ARRAY));
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
        return getResources().map(ros -> ros.getResources().stream().map(ro ->
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
