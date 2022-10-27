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
import jakarta.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
@Service
public class RegistryService {

    public static final String SERVICE_TYPE = "registry";

    private static final Logger logger = LogManager.getLogger(RegistryService.class);

    private List<ServiceResourceObject> serviceResources = new ArrayList<>();
    private Set<ServerResourceDocument> serverResourceDocuments = new LinkedHashSet<>();
    private final StandardConfiguration configuration;

    private final List<ServerResourceDocument> servers = new CopyOnWriteArrayList<>();

    @Inject
    public RegistryService(Configuration configuration,
                           Topic<ServiceResourceObject> registryTopic) {
        this.configuration = () -> configuration;
        registryTopic.publish(new ServiceResourceObject.Builder(this.configuration, SERVICE_TYPE)
                .api("registry", "api/registry")
                .websocket("registrysubscriber", "ws/registrysubscriber")
                .websocket("registrypublisher", "ws/registrypublisher")
                .build());
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

    public List<ServerResourceDocument> getServers() {
        return servers;
    }

    public Optional<ServiceResourceObject> getService(ServiceIdentifier serviceIdentifier) {
        return servers.stream()
                .map(ServerResourceDocument::resourceDocument)
                .flatMap(rd -> rd.getResources().stream())
                .flatMap(ResourceObjects::stream)
                .filter(ro -> ro.getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()))
                .findFirst().map(ServiceResourceObject::new);
    }

    @MessageReceiver
    public static class TopicReceiver {
        private Provider<RegistryService> registryServiceProvider;

        public TopicReceiver(Provider<RegistryService> registryServiceProvider) {
            this.registryServiceProvider = registryServiceProvider;
        }

        public void service(@SubscribeTo ServiceResourceObject serviceResourceObject) {
            registryServiceProvider.get().addService(serviceResourceObject);
        }

        public void server(@SubscribeTo ServerResourceDocument serverResourceDocument) {
            registryServiceProvider.get().addServer(serverResourceDocument);
        }
    }

    private void addService(ServiceResourceObject serviceResourceObject) {

        if (serviceResourceObject.getServiceIdentifier().resourceObjectIdentifier().getId().equals(configuration.getId())) {
            serviceResources.add(serviceResourceObject);
        }
    }

    private void addServer(ServerResourceDocument serverResourceDocument) {

        if (serverResourceDocument.resourceDocument().getResources()
                .map(ro -> ro.getResources().isEmpty()).orElse(false)) {
            serverResourceDocuments.remove(serverResourceDocument);
        } else {
            serverResourceDocuments.add(serverResourceDocument);
        }
    }
}
