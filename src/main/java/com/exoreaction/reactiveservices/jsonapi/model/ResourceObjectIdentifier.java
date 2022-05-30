package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;
import static java.util.Optional.ofNullable;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record ResourceObjectIdentifier(JsonObject json)
        implements JsonElement {
    public record Builder(JsonObjectBuilder builder) {

        public Builder(String type, String id) {
            this(Json.createObjectBuilder());
            builder.add("type", type)
                    .add("id", id);
        }

        public Builder(Enum<?> type, String id) {
            this(type.name(), id);
        }

        public Builder meta(Meta meta) {
            builder.add("meta", meta.json());
            return this;
        }

        public ResourceObjectIdentifier build() {
            return new ResourceObjectIdentifier(builder.build());
        }
    }

    public String getId() {
        return getString("id");
    }

    public String getType() {
        return getString("type");
    }

    public Meta getMeta() {
        return new Meta(ofNullable(object().getJsonObject("meta")).orElse(EMPTY_JSON_OBJECT));
    }
}
