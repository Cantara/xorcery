package com.exoreaction.reactiveservices.service.conductor;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Relationship;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
import com.exoreaction.reactiveservices.service.conductor.api.ConductorChange;
import com.exoreaction.reactiveservices.service.conductor.api.ConductorListener;
import com.exoreaction.reactiveservices.service.conductor.resources.model.*;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

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

    private Registry registry;
    private ServiceResourceObject sro;
    private ReactiveStreams reactiveStreams;


    private final GroupTemplates groupTemplates;
    private final Groups groups;
    private List<ConductorListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    public ConductorService(ReactiveStreams reactiveStreams, Registry registry, @Named(SERVICE_TYPE) ServiceResourceObject sro) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
        this.sro = sro;

        groups = new Groups(new Groups.GroupsListener() {
            @Override
            public void addedGroup(Group group) {
                listeners.forEach(l -> l.addedGroup(group));
                LogManager.getLogger(getClass()).info("Added group:" + group);
            }
        });
        groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {
//                listeners.forEach(l -> l.addedGroup(group))
            }
        }, groups);
    }

    @Override
    public void onStartup(Container container) {
        // Load templates
        ResourceDocument templates = new ResourceDocument(Json.createReader(getClass().getResourceAsStream("/conductor/templates.json")).readObject());

        templates.split().forEach(rd -> addTemplate(new GroupTemplate(rd)));

        sro.linkByRel("conductorevents").ifPresent(link ->
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

        addedTemplate(groupTemplate);

        LogManager.getLogger(getClass()).info("Added template:" + groupTemplate);
    }

    public void removeTemplate(String templateId) {
//        groupTemplates.removeIf(t -> t.resourceObject().getId().equals(templateId));
    }

    public List<GroupTemplate> getTemplates() {
        return groupTemplates.getTemplates();
    }

    public List<Group> getGroups() {
        return groups.getGroups();
    }

    @Override
    public void addConductorListener(ConductorListener listener) {
        groups.getGroups().forEach(listener::addedGroup);
        listeners.add(listener);
    }

    public void addedTemplate(GroupTemplate groupTemplate) {
//        listeners.forEach(l -> l.addedTemplate(group);
    }

    public void addedGroup(Group group) {
        listeners.forEach(l -> l.addedGroup(group));
    }


    private class ConductorRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {

            groupTemplates.addedService(new Service(service));
        }
    }

    private class ConductorPublisher
            implements ReactiveEventStreams.Publisher<ConductorChange> {
        @Override
        public void subscribe(ReactiveEventStreams.Subscriber<ConductorChange> subscriber, Map<String, String> parameters) {

        }
    }
}
