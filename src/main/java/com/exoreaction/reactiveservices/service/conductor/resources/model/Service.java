package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;

public class Service
{
    private final ResourceObject resourceObject;

    public Service(ResourceObject resourceObject) {
        this.resourceObject = resourceObject;
    }
}
