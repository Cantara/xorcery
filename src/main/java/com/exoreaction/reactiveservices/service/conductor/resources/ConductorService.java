package com.exoreaction.reactiveservices.service.conductor.resources;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Service;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Template;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.websocket.api.Session;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */
@Singleton
@Contract
public class ConductorService
        implements ContainerLifecycleListener {

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return "conductor";
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api("conductor", "api/conductor")
                    .websocket("conductorevents", "ws/conductorevents");
        }

        @Override
        protected void configure() {
            context.register(ConductorService.class, ConductorService.class, ContainerLifecycleListener.class);
        }
    }

    private Registry registry;
    private ReactiveStreams reactiveStreams;

    private final List<Template> templates = new CopyOnWriteArrayList<>();
    private final List<Group> groups = new CopyOnWriteArrayList<>();
    private List<Session> sessions = new CopyOnWriteArrayList<>();

    @Inject
    public ConductorService(ReactiveStreams reactiveStreams, Registry registry) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
    }

    @Override
    public void onStartup(Container container) {
        // Load templates
        ResourceDocument templates = new ResourceDocument(Json.createReader(getClass().getResourceAsStream("/conductor/templates.json")).readObject());

        templates.getResources()
                .ifPresent(ro -> ro.getResources().forEach(r -> addTemplate(new Template(r))));

//        reactiveStreams.subscribe();

//        registry.addRegistryListener(new ConductorRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public void addTemplate(Template template) {
        templates.add(template);

        addedTemplate(template);

        LogManager.getLogger(getClass()).info("Added template:" + template);
    }

    public void removeTemplate(String templateId) {
        templates.removeIf(t -> t.getId().equals(templateId));
    }

    public List<Template> getTemplates() {
        return templates;
    }

    public void addGroup(Group group) {
        groups.add(group);

        addedGroup(group);

        LogManager.getLogger(getClass()).info("Added group:" + group);
    }

    public void removeGroup(String groupId) {
        groups.removeIf(g -> g.getId().equals(groupId));
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void addSession(Session session) {
        sessions.add(session);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    public void addedTemplate(Template template) {
        send(template.getJson());
    }

    public void addedGroup(Group group) {
        send(group.getJson());
    }

    public void send(ResourceObject resourceObject) {
        final byte[] msg = resourceObject.toString().getBytes(StandardCharsets.UTF_8);
        var byteBuffer = ByteBuffer.wrap(msg);

        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            try {
                session.getRemote().sendBytes(byteBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addedService(Service service) {

    }

    private class ConductorRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {

            ConductorService.this.addedService(new Service(service));
        }
    }

}
