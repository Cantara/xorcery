package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjectIdentifier;

import java.util.List;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public interface RegistryListener {
    default void snapshot(List<ResourceDocument> servers) {
        for (ResourceDocument server : servers) {
            addedServer(server);
        }
    }

    default void addedServer(ResourceDocument server) {
        server.getResources().ifPresent(resources ->
        {
            resources.getResources().forEach(this::addedService);
        });
    }

    default void removedServer(ResourceDocument server) {
        server.getResources().ifPresent(resources ->
        {
            resources.getResources()
                    .stream()
                    .map(ResourceObject::getResourceObjectIdentifier)
                    .forEach(this::removedService);
        });
    }

    default void addedService(ResourceObject service) {

    }

    default void removedService(ResourceObjectIdentifier service) {
    }
}
