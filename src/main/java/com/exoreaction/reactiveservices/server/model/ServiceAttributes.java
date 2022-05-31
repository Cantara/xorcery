package com.exoreaction.reactiveservices.server.model;

import com.exoreaction.reactiveservices.jsonapi.model.Attributes;

import java.util.Optional;

public record ServiceAttributes(Attributes attributes) {

    public Optional<String> getVersion()
    {
        return attributes.getOptionalString("version");
    }

}
