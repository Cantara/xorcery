package com.exoreaction.reactiveservices.server.model;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;

import java.util.Optional;
import java.util.stream.Stream;

public record ServerResourceDocument(ResourceDocument resourceDocument) {

    public Stream<ServiceResourceObject> getServices() {
        return resourceDocument.getResources().orElseThrow()
                .getResources().stream()
                .map(ServiceResourceObject::new);
    }

    public Optional<ServiceResourceObject> getServiceByIdentifier(ServiceIdentifier serviceIdentifier) {
        return resourceDocument.getResources()
                .flatMap(r -> r.getResources().stream()
                        .filter(ro -> ro.getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()))
                        .findFirst())
                .map(ServiceResourceObject::new);
    }

    public Optional<ServiceResourceObject> getServiceByType(String type) {
        return resourceDocument.getResources()
                .flatMap(ro -> ro.getResources().stream()
                        .filter(r -> r.getType().equals(type))
                        .findFirst())
                .map(ServiceResourceObject::new);
    }

}
