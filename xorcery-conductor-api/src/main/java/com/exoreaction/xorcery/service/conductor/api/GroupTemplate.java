package com.exoreaction.xorcery.service.conductor.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record GroupTemplate(ResourceObject resourceObject) {

    public ServiceTemplate getSources()
    {
        return new ServiceTemplate((ObjectNode)resourceObject.getAttributes().getAttribute("sources").orElseThrow());
    }

    public ServiceTemplate getConsumers()
    {
        return new ServiceTemplate((ObjectNode)resourceObject.getAttributes().getAttribute("consumers").orElseThrow());
    }
}
