package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;

public class Group {
    private final ResourceObject resourceObject;

    public Group(ResourceObject resourceObject)
    {
        this.resourceObject = resourceObject;
    }

    public String getId() {
        return resourceObject.getId();
    }

    public ResourceObject getJson() {
        return resourceObject;
    }
}
