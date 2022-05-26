package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.Relationship;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjectIdentifier;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;

import java.util.Optional;

public record GroupTemplate(ResourceDocument resourceDocument) {
    public boolean isMatch(ServiceResourceObject service) {

        for (ResourceObjectIdentifier roi : template().getRelationships().getRelationship("services")
                .flatMap(Relationship::getResourceObjectIdentifiers).orElseThrow().getResources()) {

            if (isMatch(roi, service))
                return true;
        }

        return false;
    }

    public boolean isMany(ServiceResourceObject service) {

        for (ResourceObjectIdentifier roi : template().getRelationships().getRelationship("services")
                .flatMap(Relationship::getResourceObjectIdentifiers).orElseThrow().getResources()) {

            if (isMatch(roi, service))
            {
                return roi.getMeta().getOptionalString("cardinality").map(s-> s.equals("many")).orElse(false);
            }
        }

        return false;
    }

    public Optional<ResourceObjectIdentifier> match(ServiceResourceObject service) {

        for (ResourceObjectIdentifier roi : template().getRelationships().getRelationship("services")
                .flatMap(Relationship::getResourceObjectIdentifiers).orElseThrow().getResources()) {

            if (isMatch(roi, service))
                return Optional.of(roi);
        }

        return Optional.empty();
    }

    private boolean isMatch(ResourceObjectIdentifier serviceTemplate, ServiceResourceObject service) {
        if (serviceTemplate.getType().equals("type") &&
                service.resourceObject().getType().equals(serviceTemplate.getId()))
            return true;
        return serviceTemplate.getType().equals("rel") &&
                service.resourceObject().getLinks().getRel(serviceTemplate.getId()).isPresent();
    }

    public boolean isMatched(Group partialGroup) {
        return template().getRelationships().getRelationship("services")
                .flatMap(Relationship::getResourceObjectIdentifiers).orElseThrow().getResources()
                .stream().allMatch(serviceTemplate ->
                {
                    for (ServiceResourceObject service : partialGroup.services()) {
                        if (isMatch(serviceTemplate, service)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    public ResourceObject template() {
        return resourceDocument().getResource().orElseThrow();
    }
}
