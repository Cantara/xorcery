package com.exoreaction.reactiveservices.service.visualizer;


import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Contract
public class Visualizer
        implements ContainerLifecycleListener {

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return "visualizer";
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api("visualizer", "api/visualizer");
        }

        @Override
        protected void configure() {
            context.register(Visualizer.class, Visualizer.class, ContainerLifecycleListener.class);
        }
    }

    private ReactiveStreams reactiveStreams;
    private Registry registry;
    private final AtomicInteger serviceCounter = new AtomicInteger();
    private final List<ServiceEntry> services = new CopyOnWriteArrayList<>();

    private final List<ServiceConnection> connections = new CopyOnWriteArrayList<>();

    @Inject
    public Visualizer(ReactiveStreams reactiveStreams, Registry registry) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
    }

    @Override
    public void onStartup(Container container) {
        System.out.println("Visualizer Startup");

//        registry.addRegistryListener(new VisualizerRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

        // TODO Close active sessions
        System.out.println("Shutdown");
    }

    public List<ServiceEntry> getServices() {
        return services;
    }

    public List<ServiceConnection> getConnections() {
        return connections;
    }

    private class VisualizerRegistryListener implements RegistryListener {
        @Override
        public void addedService(ServiceResourceObject service) {
            services.add(new ServiceEntry(service, serviceCounter.incrementAndGet()));
        }
    }

    private class LogEventHandler
            implements DefaultEventHandler<Event<LogEvent>> {
        @Override
        public void onEvent(Event<LogEvent> event, long sequence, boolean endOfBatch) throws Exception {
            if (event.event.getMessage().getFormat().startsWith("Connected to")) {
                String serviceId = event.event.getMarker().getName().split(":")[1];
                String toUri = event.event.getMessage().getFormattedMessage().substring("Connected to ".length());

                int fromId = getServiceById(serviceId);
                int toId = getServiceByLink(toUri);
                if (fromId != -1 && toId != -1)
                    connections.add(new ServiceConnection(fromId, toId));
            }
        }

        private int getServiceById(String serviceId) {
            for (ServiceEntry service : services) {
                if (service.resource().serverId().equals(serviceId))
                    return service.id();
            }
            return -1;
        }

        private int getServiceByLink(String toUri) {
            for (ServiceEntry service : services) {
                for (Link link : service.resource().resourceObject().getLinks().getLinks()) {
                    if (link.getHref().equals(toUri))
                        return service.id();
                }
            }
            return -1;
        }

    }

    private record ServiceEntry(ServiceResourceObject resource, int id) {
    }

    private record ServiceConnection(int from, int to) {
    }
}
