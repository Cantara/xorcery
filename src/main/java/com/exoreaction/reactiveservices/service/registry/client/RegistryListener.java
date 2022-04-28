package com.exoreaction.reactiveservices.service.registry.client;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public interface RegistryListener {
    default void registry(ResourceDocument registry) {
    }

    default void addedServer(ResourceDocument server) {
        server.getResources().ifPresent(resources ->
        {
            resources.getResources().forEach(this::addedService);
        });
    }
    default void updatedServer(ResourceDocument server) {
        server.getResources().ifPresent(resources ->
        {
            resources.getResources().forEach(this::updatedService);
        });
    }
    default void removedServer(ResourceDocument server) {
        server.getResources().ifPresent(resources ->
        {
            resources.getResources().forEach(this::removedService);
        });
    }

    default void addedService(ResourceObject service) {

    }

    default void updatedService(ResourceObject service) {

    }

    default void removedService(ResourceObject service) {
    }
}
