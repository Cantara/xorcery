package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.jsonapi.ResourceObject;

public record ServiceLinkReference(ServiceReference service, String rel) {

    public ServiceLinkReference(ResourceObject resourceObject, String rel) {
        this(new ServiceReference(resourceObject.getResourceObjectIdentifier()), rel);
    }

    @Override
    public String toString() {
        return service.toString()+":"+rel;
    }
}
