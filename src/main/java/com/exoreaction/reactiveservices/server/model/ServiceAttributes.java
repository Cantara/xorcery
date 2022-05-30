package com.exoreaction.reactiveservices.server.model;

import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import jakarta.json.JsonNumber;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record ServiceAttributes(Attributes attributes) {

    public Optional<String> getVersion()
    {
        return attributes.getOptionalString("version");
    }

}
