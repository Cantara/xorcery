package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceReference;
import org.glassfish.jersey.spi.Contract;

import java.util.List;
import java.util.Optional;

@Contract
public interface Registry {
    // Write
    void addServer(ResourceDocument server);

    void removeServer(String serverSelfUri);

    // Read
    List<ResourceDocument> getServers();

    abstract Optional<ServiceResourceObject> getService(ServiceReference serviceReference);

    abstract Optional<Link> getServiceLink(ServiceLinkReference serviceLinkReference);

    abstract void addRegistryListener(RegistryListener listener);
}