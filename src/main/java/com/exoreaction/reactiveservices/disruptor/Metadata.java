package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.annotation.*;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Metadata(JsonObject metadata) {
    public record Builder(JsonObjectBuilder builder) {

        public Builder() {
            this(Json.createObjectBuilder());
        }

        public Builder add(String name, String value) {
            builder.add(name, value);
            return this;
        }

        public Builder add(String name, long value) {
            builder.add(name, value);
            return this;
        }

        public Builder add(String name, JsonObject value) {
            builder.add(name, value);
            return this;
        }

        public Builder add(Metadata metadata) {
            this.builder.addAll(Json.createObjectBuilder(metadata.metadata));
            return this;
        }

        public Metadata build() {
            return new Metadata(builder.build());
        }
    }

    public Optional<String> getString(String name) {
        return Optional.ofNullable(metadata.getString(name));
    }

    public Optional<Long> getLong(String name) {
        return Optional.ofNullable(metadata.getJsonNumber(name)).map(JsonNumber::longValue);
    }

    public Optional<JsonObject> getJsonObject(String name) {
        return Optional.ofNullable(metadata.getJsonObject(name));
    }

    public Builder toBuilder() {
        return new Builder(Json.createObjectBuilder(metadata));
    }
}
