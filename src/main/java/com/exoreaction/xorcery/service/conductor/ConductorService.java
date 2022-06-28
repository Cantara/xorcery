package com.exoreaction.xorcery.service.conductor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.json.VariableResolver;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.conductor.api.ConductorChange;
import com.exoreaction.xorcery.service.conductor.api.ConductorListener;
import com.exoreaction.xorcery.service.conductor.resources.model.Group;
import com.exoreaction.xorcery.service.conductor.resources.model.GroupTemplate;
import com.exoreaction.xorcery.service.conductor.resources.model.GroupTemplates;
import com.exoreaction.xorcery.service.conductor.resources.model.Groups;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.registry.api.Registry;
import com.exoreaction.xorcery.service.registry.api.RegistryListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */
@Singleton
public class ConductorService
        implements Conductor, ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "conductor";

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api("conductor", "api/conductor")
                    .websocket("conductorevents", "ws/conductorevents");
        }

        @Override
        protected void configure() {
            context.register(ConductorService.class, Conductor.class, ContainerLifecycleListener.class);
        }
    }

    private Logger logger = LogManager.getLogger(getClass());

    private Registry registry;
    private ServiceResourceObject sro;
    private Configuration configuration;
    private ReactiveStreams reactiveStreams;


    private final GroupTemplates groupTemplates;
    private final Groups groups;
    private List<ConductorListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    public ConductorService(ReactiveStreams reactiveStreams, Registry registry,
                            @Named(SERVICE_TYPE) ServiceResourceObject sro, Configuration configuration) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
        this.sro = sro;
        this.configuration = configuration;

        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        groups = new Groups(new Groups.GroupsListener() {

            @Override
            public void addedGroup(Group group) {
                listeners.forEach(l -> l.addedGroup(group, registry));
                try {
                    logger.debug("Added group:\n" + mapper.writeValueAsString(group.resourceObject().object()));
                } catch (JsonProcessingException e) {
                    // Ignore
                }
            }

            @Override
            public void updatedGroup(Group group) {
                listeners.forEach(l -> l.updatedGroup(group, registry));
                try {
                    logger.debug("Updated group:\n" + mapper.writeValueAsString(group.resourceObject().object()));
                } catch (JsonProcessingException e) {
                    // Ignore
                }
            }
        });
        groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {
                listeners.forEach(l -> l.addedTemplate(groupTemplate));
                try {
                    logger.debug("Added template:\n" + mapper.writeValueAsString(groupTemplate.resourceObject().object()));
                } catch (JsonProcessingException e) {
                    // Ignore
                }
            }
        }, groups);
    }

    @Override
    public void onStartup(Container container) {
        // Load templates
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        for (JsonNode templateJson : configuration.getList("conductor.templates").orElseThrow(()->
                new IllegalStateException("Missing conductor.templates configuration"))) {
            String templateName = templateJson.textValue();
            logger.info("Loading conductor template from:" + templateName);
            ObjectNode templateNode = null;
            try {
                URI templateUri = URI.create(templateName);
                templateNode = (ObjectNode) objectMapper.readTree(templateUri.toURL().openStream());

            } catch (IllegalArgumentException | IOException e) {
                // Just load from classpath
                try {
                    templateNode = (ObjectNode) objectMapper.readTree(getClass().getResourceAsStream(templateName));
                } catch (IOException ex) {
                    logger.error("Could not load template " + templateName, ex);
                }
            }

            if (templateNode != null) {
                templateNode = new VariableResolver().apply(configuration.json(), templateNode);
                ResourceDocument templates = new ResourceDocument(templateNode);
                templates.getResources().ifPresent(ros -> ros.forEach(ro -> addTemplate(new GroupTemplate(ro))));
                templates.getResource().ifPresent(ro -> addTemplate(new GroupTemplate(ro)));
            }
        }

        sro.getLinkByRel("conductorevents").ifPresent(link ->
        {
            reactiveStreams.publish(sro.serviceIdentifier(), link, new ConductorPublisher());
        });

        registry.addRegistryListener(new ConductorRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public void addTemplate(GroupTemplate groupTemplate) {
        groupTemplates.addTemplate(groupTemplate);
    }

    public void removeTemplate(String templateId) {
//        groupTemplates.removeIf(t -> t.resourceObject().getId().equals(templateId));
    }

    public GroupTemplates getGroupTemplates() {
        return groupTemplates;
    }

    public Groups getGroups() {
        return groups;
    }

    @Override
    public void addConductorListener(ConductorListener listener) {
        groups.getGroups().forEach(group -> listener.addedGroup(group, registry));
        listeners.add(listener);
    }

    private class ConductorRegistryListener implements RegistryListener {
        @Override
        public void snapshot(Collection<ServerResourceDocument> servers) {
            RegistryListener.super.snapshot(servers);
        }

        @Override
        public void addedService(ServiceResourceObject service) {
//            System.out.println("Conductor Service:"+service.serviceIdentifier());
            groupTemplates.addedService(service);
        }
    }

    private class ConductorPublisher
            implements ReactiveEventStreams.Publisher<ConductorChange> {
        @Override
        public void subscribe(ReactiveEventStreams.Subscriber<ConductorChange> subscriber, Configuration configuration) {

        }
    }
}
