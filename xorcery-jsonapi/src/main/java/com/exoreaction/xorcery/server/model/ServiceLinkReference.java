package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.jsonapi.model.ResourceObject;

import java.util.Objects;

public final class ServiceLinkReference {
    private final ServiceIdentifier service;
    private final String rel;

    public ServiceLinkReference(ServiceIdentifier service, String rel) {
        this.service = service;
        this.rel = rel;
    }

    public ServiceLinkReference(ResourceObject resourceObject, String rel) {
        this(new ServiceIdentifier(resourceObject.getResourceObjectIdentifier()), rel);
    }

    @Override
    public String toString() {
        return service.toString() + ":" + rel;
    }

    public ServiceIdentifier service() {
        return service;
    }

    public String rel() {
        return rel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ServiceLinkReference) obj;
        return Objects.equals(this.service, that.service) &&
               Objects.equals(this.rel, that.rel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, rel);
    }

}
