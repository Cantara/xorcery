package com.exoreaction.reactiveservices.service.registry.client;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.rest.RestClient;
import com.exoreaction.reactiveservices.rest.RestHelpers;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.StringReader;
import java.util.concurrent.CompletionStage;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class RegistryClient
        implements ContainerLifecycleListener, RestHelpers {

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        public boolean configure(FeatureContext context, InjectionManager im, Server server) {
            context.register(new RegistryClient(im.getInstance(RestClient.class), im.getInstance(Configuration.class), im.getInstance(Server.class)));
//            injectionManager.register(Bindings.service(RegistryClient.class));
            return super.configure(context, im, server);
        }

        @Override
        protected void configure() {
//            bind(RegistryClient.class).to(RegistryClient.class);
        }
    }

    private final String registryUri;
    private RestClient restClient;
    private Configuration configuration;
    private Server server;

    @Inject
    public RegistryClient(RestClient restClient, Configuration configuration, Server server) {
        this.restClient = restClient;
        this.configuration = configuration.getConfiguration("registryclient");
        this.server = server;
        System.out.println("Registry client create");

        this.registryUri = this.configuration.getString("uri", null);
    }

    @Override
    public void onStartup(Container container) {
        System.out.println("Registry client startup");

        restClient.get(registryUri)
                .thenCompose(RestHelpers::toResourceDocument)
                .thenCompose(registry ->
                {
                    // Post server description
                    Link link = registry.getLinks().getRel("servers").orElseThrow();
                    return restClient.submit(link.getHref(), server.getServerDocument());
                }).thenAccept(result ->
                {
                    switch (result.getResponse().getStatus()) {
                        case 200:
                            LogManager.getLogger(RegistryClient.class).info("Registered server");
                            break;
                        default:
                            LogManager.getLogger(RegistryClient.class).info("Failed to register server: {} {}", result.getResponse().getStatus(), result.getResponse().getReason());
                            break;
                    }

                }).exceptionally(ex ->
                {
                    // TODO Retry logic goes here
                    LogManager.getLogger(RegistryClient.class).warn("Failed to register server", ex);
                    return null;
                });

    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }


    public void addRegistryListener(RegistryListener listener) {
        // Get current registry snapshot
        restClient.get(registryUri)
                .thenCompose(RestHelpers::toResourceDocument)
                .thenCompose(registry ->
                {
                    listener.registry(registry);

                    // Register for updates
                    Link websocket = registry.getLinks().getRel("events").orElseThrow();
                    return restClient.connect(websocket.getHref(), new ClientEndpoint(listener))
                            .thenCompose(session ->
                            {
                                Link link = registry.getLinks().getRel("servers").orElseThrow();
                                return restClient.get(link.getHref())
                                        .thenCompose(RestHelpers::toResourceDocument)
                                        .thenAccept(servers ->
                                        {
                                            listener.servers(servers);
                                        });

                            });
                }).exceptionally(ex ->
                {
                    // TODO Retry logic goes here
                    LogManager.getLogger(RegistryClient.class).warn("Failed to register listener", ex);
                    return null;
                });
    }

    public CompletionStage<Session> connect(String uri, Object clientEndpoint) {
        return restClient.connect(uri, clientEndpoint);
    }

    public static class ClientEndpoint
            implements WebSocketListener {
        private final RegistryListener listener;

        public ClientEndpoint(RegistryListener listener) {
            this.listener = listener;
        }

        @Override
        public void onWebSocketText(String body) {
            ResourceDocument added = new ResourceDocument(Json.createReader(new StringReader(body)).read());
            listener.servers(added);
        }
    }
}
