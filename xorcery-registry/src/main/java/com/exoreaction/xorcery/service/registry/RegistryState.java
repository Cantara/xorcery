package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.JsonApiRels;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.Service;

import java.util.*;

@MessageReceiver({ServiceResourceObject.class, ServerResourceDocument.class})
@Service(name="registry")
public class RegistryState {

    private Set<ServerResourceDocument> serverResourceDocuments = new LinkedHashSet<>();
    private StandardConfiguration configuration;
    private ServiceResourceObjects serviceResourceObjects;

    @Inject
    public RegistryState(Configuration configuration, ServiceResourceObjects serviceResourceObjects) {
        this.configuration = ()->configuration;
        this.serviceResourceObjects = serviceResourceObjects;
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
        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {
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
