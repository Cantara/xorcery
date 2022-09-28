package com.exoreaction.xorcery.service.conductor.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.service.conductor.resources.model.Group;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AddedGroup(ObjectNode json)
        implements ConductorChange {

    Group group() {
        return new Group(new ResourceObject(json));
    }
}
