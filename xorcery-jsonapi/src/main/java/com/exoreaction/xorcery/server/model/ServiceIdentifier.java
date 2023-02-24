package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.jsonapi.model.ResourceObjectIdentifier;

import java.util.Objects;

public final class ServiceIdentifier {
    private final ResourceObjectIdentifier resourceObjectIdentifier;

    public ServiceIdentifier(ResourceObjectIdentifier resourceObjectIdentifier) {
        this.resourceObjectIdentifier = resourceObjectIdentifier;
    }

    public ServiceIdentifier(String type, String id) {
        this(new ResourceObjectIdentifier.Builder(type, id).build());
    }

    public ServiceLinkReference link(String rel) {
        return new ServiceLinkReference(this, rel);
    }

    @Override
    public String toString() {
        return resourceObjectIdentifier.getType() + ":" + resourceObjectIdentifier.getId();
    }

    public ResourceObjectIdentifier resourceObjectIdentifier() {
        return resourceObjectIdentifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ServiceIdentifier) obj;
        return Objects.equals(this.resourceObjectIdentifier, that.resourceObjectIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceObjectIdentifier);
    }

}
