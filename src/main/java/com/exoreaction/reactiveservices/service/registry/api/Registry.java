package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import org.glassfish.jersey.spi.Contract;

import java.util.List;
import java.util.Optional;

@Contract
public interface Registry {
    // Write
    void addServer(ServerResourceDocument server);

    void removeServer(String serverSelfUri);

    // Read
    List<ServerResourceDocument> getServers();

    Optional<ServiceResourceObject> getService(ServiceIdentifier serviceIdentifier);

    void addRegistryListener(RegistryListener listener);
}