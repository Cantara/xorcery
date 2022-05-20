package com.exoreaction.reactiveservices.service.conductor.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import jakarta.json.JsonValue;

public record AddedGroup(JsonValue json)
        implements ConductorChange {

    Group group() {
        return new Group(new ResourceDocument(json.asJsonObject()));
    }
}
