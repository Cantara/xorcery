package com.exoreaction.reactiveservices.service.visualizer;


import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.service.loggingconsumer.LoggingConsumerService;
import com.exoreaction.reactiveservices.service.registry.client.RegistryClient;
import com.exoreaction.reactiveservices.service.registry.client.RegistryListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
@Contract
public class VisualizerService
        implements ContainerLifecycleListener {

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server) {
            if (injectionManager.getInstance(Configuration.class).getConfiguration("visualizer").getBoolean("enabled", true)) {
                server.addService(new ResourceObject.Builder("service", "visualizer")
                        .links(new Links.Builder()
                                .link("visualizer", server.getBaseUriBuilder().path("api/visualizer")))
                        .build());

                context.register(VisualizerService.class, VisualizerService.class, ContainerLifecycleListener.class);
            }

            return super.configure(context, injectionManager, server);
        }
    }

    private RegistryClient registryClient;
    private LoggingConsumerService loggingConsumerService;

    private final AtomicInteger serviceCounter = new AtomicInteger();
    private final List<ServiceEntry> services = new CopyOnWriteArrayList<>();

    private final List<ServiceConnection> connections = new CopyOnWriteArrayList<>();

    @Inject
    public VisualizerService(RegistryClient registryClient, LoggingConsumerService loggingConsumerService) {
        this.registryClient = registryClient;
        this.loggingConsumerService = loggingConsumerService;
    }

    @Override
    public void onStartup(Container container) {
        System.out.println("Visualizer Startup");

        registryClient.addRegistryListener(new VisualizerRegistryListener());
        loggingConsumerService.addLogHandler(new LogEventHandler());
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
        public void addedService(ResourceObject service) {
            services.add(new ServiceEntry(service, serviceCounter.incrementAndGet()));
        }
    }

    private class LogEventHandler
            implements DefaultEventHandler<EventHolder<LogEvent>> {
        @Override
        public void onEvent(EventHolder<LogEvent> event, long sequence, boolean endOfBatch) throws Exception {
            if (event.event.getMessage().getFormat().startsWith("Connected to")) {
                String serviceId = event.event.getMarker().getName().split(":")[1];
                String toUri = event.event.getMessage().getFormattedMessage().substring("Connected to ".length());

                int fromId = getServiceById(serviceId);
                int toId = getServiceByLink(toUri);
                if (fromId != -1 && toId != -1)
                    connections.add(new ServiceConnection(fromId, toId));
            }
        }

        private int getServiceById(String serviceId)
        {
            for (ServiceEntry service : services) {
                if (service.resource().getId().equals(serviceId))
                    return service.id();
            }
            return -1;
        }
        private int getServiceByLink(String toUri) {
            for (ServiceEntry service : services) {
                for (Link link : service.resource().getLinks().getLinks()) {
                    if (link.getHref().equals(toUri))
                        return service.id();
                }
            }
            return -1;
        }

    }

    private record ServiceEntry(ResourceObject resource, int id)
    {
    }
    private record ServiceConnection(int from, int to) {
    }
}
