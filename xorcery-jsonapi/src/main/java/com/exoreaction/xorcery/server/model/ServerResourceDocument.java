package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class ServerResourceDocument {
    private final ResourceDocument resourceDocument;

    public ServerResourceDocument(ResourceDocument resourceDocument) {
        this.resourceDocument = resourceDocument;
    }

    public Link getSelf() {
        return resourceDocument.getLinks().getSelf().orElseThrow();
    }

    public Stream<ServiceResourceObject> getServices() {
        return resourceDocument.getResources().orElseThrow()
                .stream()
                .map(ServiceResourceObject::new);
    }

    public Optional<ServiceResourceObject> getServiceByIdentifier(ServiceIdentifier serviceIdentifier) {
        return resourceDocument.getResources()
                .flatMap(r -> r.stream()
                        .filter(ro -> ro.getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()))
                        .findFirst())
                .map(ServiceResourceObject::new);
    }

    public Optional<ServiceResourceObject> getServiceByType(String type) {
        return resourceDocument.getResources()
                .flatMap(ro -> ro.stream()
                        .filter(r -> r.getType().equals(type))
                        .findFirst())
                .map(ServiceResourceObject::new);
    }

    public ResourceDocument resourceDocument() {
        return resourceDocument;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ServerResourceDocument) obj;
        return Objects.equals(this.resourceDocument, that.resourceDocument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceDocument);
    }

    @Override
    public String toString() {
        return "ServerResourceDocument[" +
               "resourceDocument=" + resourceDocument + ']';
    }

}
