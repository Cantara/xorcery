package com.exoreaction.reactiveservices.service.reactivestreams.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjectIdentifier;

public record ServiceIdentifier(ResourceObjectIdentifier resourceObjectIdentifier)
{
    public ServiceIdentifier(String type, String id)
    {
        this(new ResourceObjectIdentifier.Builder(type, id).build());
    }

    public ServiceLinkReference link(String rel)
    {
        return new ServiceLinkReference(this, rel);
    }

    @Override
    public String toString() {
        return resourceObjectIdentifier.getType()+":"+resourceObjectIdentifier.getId();
    }
}
