package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.ResourceObject;

public class Template
{
    private ResourceObject resourceObject;

    public Template(ResourceObject resourceObject) {
        this.resourceObject = resourceObject;
    }

    public String getId() {
        return resourceObject.getId();
    }

    @Override
    public String toString() {
        return resourceObject.toString();
    }

    public ResourceObject getJson() {
        return resourceObject;
    }
}
