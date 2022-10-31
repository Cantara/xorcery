package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.JsonApiRels;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.Service;

import java.util.*;

@MessageReceiver
@Service
@Named("registry")
public class RegistryState {

    private List<ServiceResourceObject> serviceResources = new ArrayList<>();
    private Set<ServerResourceDocument> serverResourceDocuments = new LinkedHashSet<>();
    private StandardConfiguration configuration;

    @Inject
    public RegistryState(Configuration configuration) {
        this.configuration = ()->configuration;
    }

    public void service(@SubscribeTo ServiceResourceObject serviceResourceObject) {
        if (serviceResourceObject.getServiceIdentifier().resourceObjectIdentifier().getId().equals(configuration.getId())) {
            serviceResources.add(serviceResourceObject);
        }
    }

    public void server(@SubscribeTo ServerResourceDocument serverResourceDocument) {
        if (serverResourceDocument.resourceDocument().getResources()
                .map(ro -> ro.getResources().isEmpty()).orElse(false)) {
            serverResourceDocuments.remove(serverResourceDocument);
        } else {
            serverResourceDocuments.add(serverResourceDocument);
        }
    }

    public ServerResourceDocument getServer() {
        ResourceObjects.Builder builder = new ResourceObjects.Builder();
        for (ServiceResourceObject serviceResource : serviceResources) {
            builder.resource(serviceResource.resourceObject());
        }
        ResourceDocument serverDocument = new ResourceDocument.Builder()
                .links(new Links.Builder().link(JsonApiRels.self, configuration.getServerUri()).build())
                .data(builder.build())
                .build();
        return new ServerResourceDocument(serverDocument);
    }

    public Collection<ServerResourceDocument> getServers() {
        return serverResourceDocuments;
    }

    public Optional<ServiceResourceObject> getService(ServiceIdentifier serviceIdentifier) {
        return serverResourceDocuments.stream()
                .map(ServerResourceDocument::resourceDocument)
                .flatMap(rd -> rd.getResources().stream())
                .flatMap(ResourceObjects::stream)
                .filter(ro -> ro.getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()))
                .findFirst().map(ServiceResourceObject::new);
    }


}
