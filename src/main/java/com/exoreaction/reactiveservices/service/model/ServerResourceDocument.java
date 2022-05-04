package com.exoreaction.reactiveservices.service.model;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;

import java.util.Optional;

public record ServerResourceDocument(ResourceDocument resourceDocument) {
    public Optional<ServiceResourceObject> serviceByType(String type) {
        return resourceDocument.getResources()
                .flatMap(ro -> ro.getResources().stream()
                        .filter(r -> r.getType().equals(type))
                        .findFirst())
                .map(ServiceResourceObject::new);
    }
}
