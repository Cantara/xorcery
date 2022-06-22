package com.exoreaction.xorcery.service.registry.api;

import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;

import java.util.Collection;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public interface RegistryListener {
    default void snapshot(Collection<ServerResourceDocument> servers) {
        servers.forEach(this::addedServer);
    }

    default void addedServer(ServerResourceDocument server) {
        server.getServices().forEach(this::addedService);
    }

    default void removedServer(ServerResourceDocument server) {
        server.getServices().map(ServiceResourceObject::serviceIdentifier).forEach(this::removedService);
    }

    default void addedService(ServiceResourceObject service) {

    }

    default void removedService(ServiceIdentifier service) {
    }
}
