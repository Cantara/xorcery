package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjectIdentifier;

public record ServiceReference(String type, String id)
{
    public ServiceReference(ResourceObjectIdentifier resourceObjectIdentifier)
    {
        this(resourceObjectIdentifier.getType(), resourceObjectIdentifier.getId());
    }

    public ServiceLinkReference link(String rel)
    {
        return new ServiceLinkReference(this, rel);
    }

    @Override
    public String toString() {
        return id+":"+type;
    }
}
