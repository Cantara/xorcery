package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record Meta(JsonObject json)
        implements JsonElement {
    public record Builder(JsonObjectBuilder json) {
        public Builder() {
            this(Json.createObjectBuilder());
        }

        public Builder meta(String name, JsonValue value) {
            json.add(name, value);
            return this;
        }

        public Builder meta(String name, long value) {
            json.add(name, value);
            return this;
        }

        public Builder with(Consumer<Builder> consumer) {
            consumer.accept(this);
            return this;
        }

        public Meta build() {
            return new Meta(json.build());
        }
    }

    public JsonObject getMeta() {
        return json().asJsonObject();
    }
}
