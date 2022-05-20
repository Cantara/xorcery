package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;

public record ServiceLinkReference(ServiceIdentifier service, String rel) {

    public ServiceLinkReference(ResourceObject resourceObject, String rel) {
        this(new ServiceIdentifier(resourceObject.getResourceObjectIdentifier()), rel);
    }

    @Override
    public String toString() {
        return service.toString()+":"+rel;
    }
}
