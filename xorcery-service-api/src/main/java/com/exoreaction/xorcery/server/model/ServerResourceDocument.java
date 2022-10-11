package com.exoreaction.xorcery.server.model;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;

import java.util.Optional;
import java.util.stream.Stream;

public record ServerResourceDocument(ResourceDocument resourceDocument) {

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
}
