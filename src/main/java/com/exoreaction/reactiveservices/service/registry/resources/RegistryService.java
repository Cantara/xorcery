package com.exoreaction.reactiveservices.service.registry.resources;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.service.helpers.ServiceResourceObjectBuilder;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceReference;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.registry.resources.websocket.RegistryWebSocketServlet;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public class RegistryService
        implements ContainerLifecycleListener {
    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        protected String serviceType() {
            return "registry";
        }

        @Override
        protected void buildResourceObject(ServiceResourceObjectBuilder builder) {
            builder.api("registry", "api/registry")
                    .websocket("registryevents", "ws/registryevents");
        }

        @Override
        protected void configure() {
            RegistryService service = new RegistryService();
            context.register(service);
            injectionManager.getInstance(ServletContextHandler.class).addServlet(new ServletHolder(new RegistryWebSocketServlet(service)), "/ws/registryevents");
        }
    }

    private final List<ResourceDocument> servers = new CopyOnWriteArrayList<>();
    private List<Session> sessions = new CopyOnWriteArrayList<>();

    public RegistryService() {
    }

    @Override
    public void onStartup(Container container) {
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public void addServer(ResourceDocument server) {
        servers.add(server);

        send(server);

        LogManager.getLogger(getClass()).debug("Added server:" + server);
    }

    public void removeServer(String serverSelfUri) {
        servers.removeIf(resourceDocument -> resourceDocument.getLinks().getSelf()
                .map(link -> link.getHref().equals(serverSelfUri))
                .orElse(false));
    }

    public List<ResourceDocument> getServers() {
        return servers;
    }

    public Optional<ResourceObject> getService(ServiceReference serviceReference)
    {
        return servers.stream()
                .flatMap(rd -> rd.getResources().stream())
                .flatMap(r -> r.getResources().stream())
                .filter(ro -> ro.getId().equals(serviceReference.id()) && ro.getType().equals(serviceReference.type()))
                .findFirst();
    }

    public Optional<Link> getServiceLink(ServiceLinkReference serviceLinkReference)
    {
        return getService(serviceLinkReference.service())
                .flatMap(ro -> ro.getLinks().getRel(serviceLinkReference.rel()));
    }

    public void addSession(Session session) {
        sessions.add(session);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    public void send(ResourceDocument resourceObject) {
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
}
