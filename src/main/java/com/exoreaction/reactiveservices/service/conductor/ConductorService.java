package com.exoreaction.reactiveservices.service.conductor;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
import com.exoreaction.reactiveservices.service.conductor.api.ConductorChange;
import com.exoreaction.reactiveservices.service.conductor.api.ConductorListener;
import com.exoreaction.reactiveservices.service.conductor.resources.model.*;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

        groups = new Groups(new Groups.GroupsListener() {
            @Override
            public void addedGroup(Group group) {
                listeners.forEach(l -> l.addedGroup(group));
                logger.info("Added group:" + group);
            }

            @Override
            public void updatedGroup(Group group) {
                listeners.forEach(l -> l.updatedGroup(group));
                logger.info("Updated group:" + group);
            }
        });
        groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {
                listeners.forEach(l -> l.addedTemplate(groupTemplate));
                logger.info("Added template:" + groupTemplate);
            }
        }, groups, configuration);
    }

    @Override
    public void onStartup(Container container) {
        // Load templates
        for (String templateName : List.of(configuration.getString("conductor.templates").orElseThrow().split(","))) {
            logger.info("Loading conductor template from:" + templateName);
            try {
                URI templateUri = URI.create(templateName);

                ResourceDocument templates = new ResourceDocument(Json.createReader(templateUri.toURL().openStream()).readObject());
                templates.getResources().ifPresent(ros -> ros.getResources().forEach(ro -> addTemplate(new GroupTemplate(ro))));
                templates.getResource().ifPresent(ro -> addTemplate(new GroupTemplate(ro)));
            } catch (IllegalArgumentException | IOException e) {
                // Just load from classpath
                ResourceDocument templates = new ResourceDocument(Json.createReader(getClass().getResourceAsStream(templateName)).readObject());
                templates.getResources().ifPresent(ros -> ros.getResources().forEach(ro -> addTemplate(new GroupTemplate(ro))));
                templates.getResource().ifPresent(ro -> addTemplate(new GroupTemplate(ro)));
            }
        }

        sro.getLinkByRel("conductorevents").ifPresent(link ->
        {
            reactiveStreams.publish(sro.serviceIdentifier(), link, new ConductorPublisher());
        });

        registry.addRegistryListener(new ConductorRegistryListener());
        System.out.println("Added conductor registry listener");
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
        groups.getGroups().forEach(listener::addedGroup);
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
        public void subscribe(ReactiveEventStreams.Subscriber<ConductorChange> subscriber, JsonObject parameters) {

        }
    }
}
