package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.jsonapi.model.Attributes;

import java.util.Optional;

public record ServiceAttributes(Attributes attributes) {

    public Optional<String> getVersion()
    {
        return attributes.getOptionalString("version");
    }

}
