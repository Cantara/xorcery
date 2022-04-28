package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.jsonapi.ResourceObjectIdentifier;

public record ServiceReference(String type, String id)
{
    public ServiceReference(ResourceObjectIdentifier resourceObjectIdentifier)
    {
        this(resourceObjectIdentifier.getType(), resourceObjectIdentifier.getId());
    }

    @Override
    public String toString() {
        return id+":"+type;
    }
}
