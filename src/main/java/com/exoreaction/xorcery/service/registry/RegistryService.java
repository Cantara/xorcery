package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.jaxrs.readers.JsonApiMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.rest.RestProcess;
import com.exoreaction.xorcery.server.Server;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.xorcery.service.reactivestreams.api.SubscriberEventSink;
import com.exoreaction.xorcery.service.registry.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
@Contract
@Priority(1)
public class RegistryService
        implements Registry, ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "registry";

    private static final Logger logger = LogManager.getLogger(RegistryService.class);

    private final Disruptor<Event<RegistryChange>> disruptor;
    private ServiceResourceObject resourceObject;
    private ReactiveStreams reactiveStreams;
    private final Configuration configuration;
    private final JsonApiClient jsonApiClient;
    private final Server server;

    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api("registry", "api/registry")
                    .websocket("registryevents", "ws/registryevents");
        }

        @Override
        protected void configure() {
            JettyHttpClientContract instance = injectionManager.getInstance(JettyHttpClientContract.class);
            Client client = ClientBuilder.newBuilder().withConfig(new ClientConfig()
                    .register(new JsonApiMessageBodyReader(new ObjectMapper()))
                    .register(LoggingFeature.class)
                    .register(instance)
                    .connectorProvider(new JettyConnectorProvider())
                    .property( "jersey.config.logging.logger.level", "INFO" )
            ).build();
            bind(new JsonApiClient(client));

            context.register(RegistryService.class, Registry.class, ContainerLifecycleListener.class);

            if (configuration().getBoolean("registry.dnsdiscovery").orElse(false)) {
                context.register(RegistryDnsDiscovery.class, ContainerLifecycleListener.class);
            }
        }
    }

    private final Link registryLink;
    private StartupRegistration registration;
    private final List<ServerResourceDocument> servers = new CopyOnWriteArrayList<>();

    private final List<ServiceResourceObject> services = new CopyOnWriteArrayList<>();
    private List<SubscriberEventSink<RegistryChange>> subscribers = new CopyOnWriteArrayList<>();

    private List<RegistryListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    public RegistryService(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                           ReactiveStreams reactiveStreams,
                           Configuration configuration,
                           JsonApiClient jsonApiClient,
                           Server server,
                           ServletContextHandler servletContextHandler) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;
        this.configuration = configuration;
        this.jsonApiClient = jsonApiClient;
        this.server = server;

        this.registryLink = new Link("master", this.configuration.getString("registry.master").orElseThrow());

        disruptor = new Disruptor<>(Event::new, 16, new NamedThreadFactory("RegistryChanges"));
        UpstreamSubscriber upstreamSubscriber = new UpstreamSubscriber(disruptor.getRingBuffer());
        disruptor.handleEventsWith(upstreamSubscriber);
        disruptor.start();

        registration = new StartupRegistration(
                reactiveStreams, jsonApiClient, resourceObject.serviceIdentifier(), registryLink,
                server.getServerDocument(), upstreamSubscriber,
                new CompletableFuture<ResourceObject>().thenApply(v ->
                {
                    LogManager.getLogger(RegistryService.class).info("Registered server");
                    return v;
                }));
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.getLinkByRel("registryevents").ifPresent(link ->
        {
            reactiveStreams.publish(resourceObject.serviceIdentifier(), link, new RegistryPublisher());
        });

/*
        ForkJoinPool.commonPool().execute(()->
        {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
*/
        registration.start();
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        registration.stop();

        for (SubscriberEventSink<RegistryChange> subscriber : subscribers) {
            subscriber.subscriber().onComplete();
        }

        disruptor.shutdown();
    }

    @Override
    public void addRegistryListener(RegistryListener listener) {
        applyChange(new AddedListener(listener));
    }

    @Override
    public void addServer(ServerResourceDocument server) {
        applyChange(new AddedServer(server.resourceDocument().object()));
    }

    @Override
    public void removeServer(String serverSelfUri) {
        servers.forEach(rd ->
        {
            if (rd.resourceDocument().getLinks().getSelf()
                    .map(s -> s.getHref().equals(serverSelfUri)).orElse(false)) {
                applyChange(new RemovedServer(rd.resourceDocument().json()));
            }
        });
    }

    private void publish(RegistryChange registryChange) {

        if (!(registryChange instanceof AddedListener)) {
            for (SubscriberEventSink<RegistryChange> subscriber : subscribers) {
                subscriber.sink().publishEvent((event, seq, rc) ->
                {
                    event.event = rc;
                }, registryChange);
            }
        }
    }

    @Override
    public List<ServerResourceDocument> getServers() {
        return servers;
    }

    @Override
    public Optional<ServiceResourceObject> getService(ServiceIdentifier serviceIdentifier) {
        return servers.stream()
                .map(ServerResourceDocument::resourceDocument)
                .flatMap(rd -> rd.getResources().stream())
                .flatMap(ResourceObjects::stream)
                .filter(ro -> ro.getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()))
                .findFirst().map(ServiceResourceObject::new);
    }

    private void applyChange(RegistryChange change) {
        registration.result().whenComplete((registry, throwable) ->
        {
            disruptor.getRingBuffer().publishEvent((e, seq, l) ->
            {
                e.event = change;
            }, change);
        });
    }

    private void handleChange(RegistryChange event) {
        logger.debug("Registry upstream event:" + event);
        if (event instanceof AddedServer addedServer) {
            {
                String selfHref = addedServer.server().resourceDocument()
                        .getLinks().getSelf().map(Link::getHref).orElse("");
                servers.removeIf(rd ->
                        rd.resourceDocument().getLinks().getSelf()
                                .map(s -> s.getHref().equals(selfHref)).orElse(false));

                servers.add(addedServer.server());
                listeners.forEach(listener -> listener.addedServer(addedServer.server()));
            }
        } else if (event instanceof RegistrySnapshot registrySnapshot) {
            {
                servers.clear();
                servers.addAll(registrySnapshot.servers());
                listeners.forEach(listener -> listener.snapshot(registrySnapshot.servers()));
            }
        } else if (event instanceof RemovedServer removedServer) {
            removedServer.server().resourceDocument().getLinks().getSelf().ifPresent(self ->
            {
                for (int i = 0; i < servers.size(); i++) {
                    ResourceDocument resourceDocument = servers.get(i).resourceDocument();
                    if (resourceDocument.getLinks().getSelf().map(link -> link.getHref().equals(removedServer)).orElse(false)) {
                        ServerResourceDocument srd = servers.remove(i);
                        listeners.forEach(listener -> listener.removedServer(srd));
                        break;
                    }
                }
            });
        } else if (event instanceof AddedListener listener) {
            RegistryListener registryListener = listener.listener();
            listeners.add(registryListener);
            registryListener.snapshot(servers);
        }

        // Publish event downstream
        publish(event);
    }

    private class RegistryPublisher
            implements ReactiveEventStreams.Publisher<RegistryChange> {
        @Override
        public void subscribe(ReactiveEventStreams.Subscriber<RegistryChange> subscriber, Configuration parameters) {
            AtomicReference<SubscriberEventSink<RegistryChange>> eventSink = new AtomicReference<>();

            eventSink.set(new SubscriberEventSink<>(subscriber, subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
                @Override
                public void request(long n) {

                }

                @Override
                public void cancel() {
                    subscribers.remove(eventSink.get());
                    subscriber.onComplete();
                }
            })));

            subscribers.add(eventSink.get());

            ResourceObjects.Builder resourceObjects = new ResourceObjects.Builder();
            for (ServerResourceDocument serverResourceDocument : servers) {
                serverResourceDocument.resourceDocument().getResources().ifPresent(ros ->
                {
                    ros.forEach(resourceObjects::resource);
                });
            }
            ResourceDocument snapshotDocument = new ResourceDocument.Builder()
                    .data(resourceObjects.build())
                    .build();

            ArrayNode serverDocuments = JsonNodeFactory.instance.arrayNode();
            servers.forEach(srd -> serverDocuments.add(srd.resourceDocument().json()));

            RegistrySnapshot snapshot = new RegistrySnapshot(serverDocuments);
            eventSink.get().sink().publishEvent((event, seq, rc) ->
            {
                event.event = rc;
            }, snapshot);
            logger.debug("Sent registry snapshot:" + snapshot);
        }
    }

    public record StartupRegistration(ReactiveStreams reactiveStreams, JsonApiClient client,
                                      ServiceIdentifier serviceIdentifier,
                                      Link registryUri,
                                      ResourceDocument server,
                                      UpstreamSubscriber upstreamSubscriber,
                                      CompletionStage<ResourceObject> result)
            implements RestProcess<ResourceObject> {

        public void start() {
            client.get(registryUri)
                    .thenApply(ServerResourceDocument::new)
                    .thenCompose(this::subscribeMaster)
                    .thenCompose(this::submitServer)
                    .whenComplete(this::complete);
        }

        private CompletionStage<ServerResourceDocument> subscribeMaster(ServerResourceDocument registry) {

            return registry.getServiceByType("registry")
                    .map(sro ->
                            {
                                if (sro.serviceIdentifier().equals(serviceIdentifier)) {
                                    // Master is localhost, no need to subscribe
                                    return CompletableFuture.completedStage(registry);
                                } else {

                                    return sro.getLinkByRel("registryevents").map(link ->
                                    {
                                        logger.info("Subscribing to upstream registry");
                                        reactiveStreams.subscribe(serviceIdentifier, link, upstreamSubscriber, new Configuration(JsonNodeFactory.instance.objectNode()));
                                        return CompletableFuture.completedStage(registry);
                                    }).orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No link 'registryevents' in registry")));
                                }
                            }
                    ).orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No service 'registry' in master")));
        }

        private <U> CompletionStage<ResourceObject> submitServer(ServerResourceDocument registry) {
            return registry.getServiceByType("registry")
                    .map(sro -> sro.getLinkByRel("registry").map(link ->
                            client.get(link)
                                    .thenCompose(rd -> rd.getLinks().getByRel("servers").map(serversLink ->
                                            client.submit(serversLink, server)).orElse(CompletableFuture.failedStage(new IllegalStateException("No link 'servers' in registry"))))).orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No link 'registryevents' in registry"))))
                    .orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No service 'registry' in master")));
        }

        @Override
        public void complete(ResourceObject value, Throwable t) {
            logger.info("Startup registration complete");
            RestProcess.super.complete(value, t);
        }
    }

    public record ShutdownDeregistration(JsonApiClient client, Link registryUri,
                                         ResourceDocument server,
                                         CompletionStage<ResourceObject> result)
            implements RestProcess<ResourceObject> {

        public void start() {
            client.get(registryUri)
                    .thenApply(ServerResourceDocument::new)
                    .thenCompose(this::deleteServer)
                    .whenComplete(this::complete);
        }

        private <U> CompletionStage<ResourceObject> deleteServer(ServerResourceDocument registry) {
            return registry.getServiceByType("registry")
                    .map(sro -> sro.getLinkByRel("registry").map(link ->
                            client.get(link)
                                    .thenCompose(rd -> rd.getLinks().getByRel("servers").map(serversLink ->
                                            client.submit(serversLink, server)).orElse(CompletableFuture.failedStage(new IllegalStateException("No link 'servers' in registry"))))).orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No link 'registryevents' in registry"))))
                    .orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No service 'registry' in master")));
        }
    }

    private class UpstreamSubscriber
            implements ReactiveEventStreams.Subscriber<RegistryChange>, EventHandler<Event<RegistryChange>> {

        private RingBuffer<Event<RegistryChange>> disruptor;
        private ReactiveEventStreams.Subscription subscription;

        public UpstreamSubscriber(RingBuffer<Event<RegistryChange>> disruptor) {
            this.disruptor = disruptor;
        }

        @Override
        public EventSink<Event<RegistryChange>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            logger.debug("Registry upstream onSubscribe");
            this.subscription = subscription;
            subscription.request(1);
            return disruptor;
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void onEvent(Event<RegistryChange> event, long sequence, boolean endOfBatch) throws Exception {
            handleChange(event.event);
            if (subscription != null) // Might be local event publish before subscription is established
                subscription.request(1);
        }
    }

    private record AddedListener(RegistryListener listener)
            implements RegistryChange {
        @Override
        public ObjectNode json() {
            return null;
        }
    }

}
