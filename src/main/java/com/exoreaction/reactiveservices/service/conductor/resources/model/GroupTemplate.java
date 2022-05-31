package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record GroupTemplate(ResourceObject resourceObject) {

    public TemplateSources getSources()
    {
        return new TemplateSources((ObjectNode)resourceObject.getAttributes().getAttribute("sources").orElseThrow());
    }

    public TemplateConsumers getConsumers()
    {
        return new TemplateConsumers((ObjectNode)resourceObject.getAttributes().getAttribute("consumers").orElseThrow());
    }
}
