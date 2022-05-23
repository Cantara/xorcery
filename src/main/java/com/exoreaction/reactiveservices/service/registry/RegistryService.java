package com.exoreaction.reactiveservices.service.registry;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jaxrs.readers.JsonApiMessageBodyReader;
import com.exoreaction.reactiveservices.jsonapi.client.JsonApiClient;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.rest.RestProcess;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.model.ServerResourceDocument;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.reactiveservices.service.reactivestreams.api.SubscriberEventSink;
import com.exoreaction.reactiveservices.service.registry.api.*;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.*;
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
public class RegistryService
        implements Registry, ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "registry";
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
            ClientConfig config = new ClientConfig();
            config.register(new JsonApiMessageBodyReader());
            Client client = ClientBuilder.newBuilder().withConfig(config).build();
            bind(new JsonApiClient(client));

            context.register(RegistryService.class, Registry.class, ContainerLifecycleListener.class);
        }
    }

    private final Link registryLink;
    private StartupRegistration registration;
    private final List<ResourceDocument> servers = new CopyOnWriteArrayList<>();

    private final List<ServiceResourceObject> services = new CopyOnWriteArrayList<>();
    private List<SubscriberEventSink<RegistryChange>> subscribers = new CopyOnWriteArrayList<>();

    private List<RegistryListener> listeners = new ArrayList<>();

    @Inject
    public RegistryService(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                           ReactiveStreams reactiveStreams,
                           Configuration configuration,
                           JsonApiClient jsonApiClient,
                           Server server) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;
        this.configuration = configuration.getConfiguration("registry");
        this.jsonApiClient = jsonApiClient;
        this.server = server;

        this.registryLink = new Link("self", this.configuration.getString("master").orElseThrow());

        registration = new StartupRegistration(
                reactiveStreams, jsonApiClient, resourceObject.serviceIdentifier(), registryLink,
                server.getServerDocument(), new UpstreamSubscriber(),
                new CompletableFuture<ResourceDocument>().thenApply(v ->
                {
                    LogManager.getLogger(RegistryService.class).info("Registered server");
                    return v;
                }));
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.linkByRel("registryevents").ifPresent(link ->
        {
            reactiveStreams.publish(resourceObject.serviceIdentifier(), link, new RegistryPublisher());
        });

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
    }

    @Override
    public void addServer(ResourceDocument server) {
        String selfHref = server.getLinks().getSelf().map(Link::getHref).orElse("");
        servers.removeIf(rd ->
                rd.getLinks().getSelf()
                        .map(s -> s.getHref().equals(selfHref)).orElse(false));

        servers.add(server);

        publish(new AddedServer(server.object()));
    }

    @Override
    public void removeServer(String serverSelfUri) {
        if (servers.removeIf(rd ->
                rd.getLinks().getSelf()
                        .map(s -> s.getHref().equals(serverSelfUri)).orElse(false))) {
            publish(new RemovedServer(Json.createValue(serverSelfUri)));
        }
    }

    private void publish(RegistryChange registryChange) {

        for (SubscriberEventSink<RegistryChange> subscriber : subscribers) {
            subscriber.sink().publishEvent((event, seq, sro) ->
            {
                event.event = registryChange;
            }, resourceObject);
        }
    }

    @Override
    public List<ResourceDocument> getServers() {
        return servers;
    }

    @Override
    public Optional<ServiceResourceObject> getService(ServiceIdentifier serviceIdentifier) {
        return servers.stream()
                .flatMap(rd -> rd.getResources().stream())
                .flatMap(r -> r.getResources().stream())
                .filter(ro -> ro.getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()))
                .findFirst().map(ServiceResourceObject::new);
    }

    @Override
    public Optional<Link> getServiceLink(ServiceLinkReference serviceLinkReference) {
        return getService(serviceLinkReference.service())
                .flatMap(ro -> ro.resourceObject().getLinks().getRel(serviceLinkReference.rel()));
    }

    @Override
    public void addRegistryListener(RegistryListener listener) {
        registration.result().whenComplete((registry, throwable) ->
        {
            listener.snapshot(servers);
            listeners.add(listener);
        });
    }

    private void handleUpstreamChange(RegistryChange event) {
        if (event instanceof AddedServer) {
            ResourceDocument resourceDocument = new ResourceDocument((JsonObject) event.json());
            {
                servers.add(resourceDocument);
                listeners.forEach(listener -> listener.addedServer(resourceDocument));
            }
        } else if (event instanceof RegistrySnapshot) {
            {
                RegistrySnapshot registrySnapshot = (RegistrySnapshot) event;
                servers.clear();
                servers.addAll(((JsonArray) registrySnapshot.json()).getValuesAs(json -> new ResourceDocument((JsonObject) json)));
//                listeners.forEach(listener -> listener.snapshot()removedServer(resourceDocument));
            }
        } else if (event instanceof RemovedServer) {
            String removedServer = ((JsonString) event.json()).getString();
            for (int i = 0; i < servers.size(); i++) {
                ResourceDocument resourceDocument = servers.get(i);
                if (resourceDocument.getLinks().getSelf().map(link -> link.getHref().equals(removedServer)).orElse(false)) {
                    servers.remove(i);
                    listeners.forEach(listener -> listener.removedServer(resourceDocument));
                    break;
                }
            }
        }
    }

    private class RegistryPublisher
            implements ReactiveEventStreams.Publisher<RegistryChange> {
        @Override
        public void subscribe(ReactiveEventStreams.Subscriber<RegistryChange> subscriber, Map<String, String> parameters) {
            AtomicReference<SubscriberEventSink<RegistryChange>> eventSink = new AtomicReference<>();

            eventSink.set(new SubscriberEventSink<>(subscriber, subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
                @Override
                public void request(long n) {

                }

                @Override
                public void cancel() {
                    subscribers.remove(eventSink.get());
                }
            })));

            subscribers.add(eventSink.get());
        }
    }

    public record StartupRegistration(ReactiveStreams reactiveStreams, JsonApiClient client,
                                      ServiceIdentifier serviceIdentifier,
                                      Link registryUri,
                                      ResourceDocument server,
                                      UpstreamSubscriber upstreamSubscriber,
                                      CompletionStage<ResourceDocument> result)
            implements RestProcess<ResourceDocument> {

        public void start() {
            client.get(registryUri)
                    .thenApply(ServerResourceDocument::new)
                    .thenCompose(this::subscribeMaster)
                    .thenCompose(this::submitServer)
                    .whenComplete(this::complete);
        }

        private CompletionStage<ServerResourceDocument> subscribeMaster(ServerResourceDocument registry) {

            return registry.serviceByType("registry")
                    .map(sro -> sro.linkByRel("registryevents").map(link ->
                    {
                        reactiveStreams.subscribe(serviceIdentifier, link, upstreamSubscriber, Collections.emptyMap());
                        return CompletableFuture.completedStage(registry);
                    }).orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No link 'registryevents' in registry"))))
                    .orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No service 'registry' in master")));
        }

        private <U> CompletionStage<ResourceDocument> submitServer(ServerResourceDocument registry) {
            return registry.serviceByType("registry")
                    .map(sro -> sro.linkByRel("registry").map(link ->
                            client.get(link)
                                    .thenCompose(rd -> rd.getLinks().getRel("servers").map(serversLink ->
                                            client.submit(serversLink, server)).orElse(CompletableFuture.failedStage(new IllegalStateException("No link 'servers' in registry"))))).orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No link 'registryevents' in registry"))))
                    .orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No service 'registry' in master")));
        }
    }

    public record ShutdownDeregistration(JsonApiClient client, Link registryUri,
                                         ResourceDocument server,
                                         CompletionStage<ResourceDocument> result)
            implements RestProcess<ResourceDocument> {

        public void start() {
            client.get(registryUri)
                    .thenApply(ServerResourceDocument::new)
                    .thenCompose(this::deleteServer)
                    .whenComplete(this::complete);
        }

        private <U> CompletionStage<ResourceDocument> deleteServer(ServerResourceDocument registry) {
            return registry.serviceByType("registry")
                    .map(sro -> sro.linkByRel("registry").map(link ->
                            client.get(link)
                                    .thenCompose(rd -> rd.getLinks().getRel("servers").map(serversLink ->
                                            client.submit(serversLink, server)).orElse(CompletableFuture.failedStage(new IllegalStateException("No link 'servers' in registry"))))).orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No link 'registryevents' in registry"))))
                    .orElseGet(() -> CompletableFuture.failedStage(new IllegalStateException("No service 'registry' in master")));
        }
    }

    private class UpstreamSubscriber
            implements ReactiveEventStreams.Subscriber<RegistryChange>, EventHandler<Event<RegistryChange>> {

        private Disruptor<Event<RegistryChange>> disruptor;
        private ReactiveEventStreams.Subscription subscription;

        @Override
        public EventSink<Event<RegistryChange>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;
            disruptor = new Disruptor<>(Event::new, 16, new NamedThreadFactory("UpstreamSubscriber"));
            disruptor.handleEventsWith(this);
            disruptor.start();
            subscription.request(1);
            return disruptor.getRingBuffer();
        }

        @Override
        public void onComplete() {
            ReactiveEventStreams.Subscriber.super.onComplete();
        }

        @Override
        public void onEvent(Event<RegistryChange> event, long sequence, boolean endOfBatch) throws Exception {
            handleUpstreamChange(event.event);
            subscription.request(1);
        }
    }
}
