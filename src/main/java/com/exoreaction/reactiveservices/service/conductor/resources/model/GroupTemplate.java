package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.Relationship;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjectIdentifier;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;

import java.util.Optional;

public record GroupTemplate(ResourceObject resourceObject) {

    public TemplateSources getSources()
    {
        return new TemplateSources(resourceObject.getAttributes().getAttribute("sources").orElseThrow().asJsonObject());
    }

    public TemplateConsumers getConsumers()
    {
        return new TemplateConsumers(resourceObject.getAttributes().getAttribute("consumers").orElseThrow().asJsonObject());
    }
}
