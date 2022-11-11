package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.*;
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
import java.util.concurrent.atomic.AtomicReference;

@MessageReceiver({ServiceResourceObject.class, ServerResourceDocument.class})
@Service(name="registry")
public class RegistryState {

    private Set<ServerResourceDocument> serverResourceDocuments = new LinkedHashSet<>();
    private StandardConfiguration configuration;
    private ServiceResourceObjects serviceResourceObjects;
    private AtomicReference<ServerResourceDocument> selfServerDocument = new AtomicReference<>();

    @Inject
    public RegistryState(Configuration configuration, ServiceResourceObjects serviceResourceObjects) {
        this.configuration = ()->configuration;
        this.serviceResourceObjects = serviceResourceObjects;
    }

    public void server(@SubscribeTo ServerResourceDocument serverResourceDocument) {
        if (serverResourceDocument.resourceDocument().getResources()
                .map(ro -> ro.getResources().isEmpty()).orElse(false)) {
            serverResourceDocuments.removeIf(srd -> srd.getSelf().equals(serverResourceDocument.getSelf()));
        } else {
            serverResourceDocuments.add(serverResourceDocument);
        }
    }

    public ServerResourceDocument getServer() {
        return selfServerDocument.updateAndGet(v ->
        {
            if (v != null) return v;

            ResourceObjects.Builder builder = new ResourceObjects.Builder();
            for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {
                builder.resource(serviceResource.resourceObject());
            }
            ResourceDocument serverDocument = new ResourceDocument.Builder()
                    .links(new Links.Builder().link(JsonApiRels.self, configuration.getServerUri()).build())
                    .meta(new Meta.Builder()
                            .meta("timestamp", System.currentTimeMillis())
                            .build())
                    .data(builder.build())
                    .build();
            return new ServerResourceDocument(serverDocument);
        });
    }

    public Collection<ServerResourceDocument> getServers() {
        return serverResourceDocuments;
    }
}
