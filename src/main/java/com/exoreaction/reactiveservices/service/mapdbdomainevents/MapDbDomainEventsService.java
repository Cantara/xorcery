package com.exoreaction.reactiveservices.service.mapdbdomainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.disruptor.MetadataDeserializerEventHandler;
import com.exoreaction.reactiveservices.disruptor.WebSocketFlowControlEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.configuration.Configuration;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.exoreaction.reactiveservices.service.mapdbdomainevents.disruptor.DomainEventDeserializeEventHandler;
import com.exoreaction.reactiveservices.service.mapdbdomainevents.disruptor.MapDbDomainEventEventHandler;
import com.exoreaction.reactiveservices.service.registry.client.RegistryClient;
import com.exoreaction.reactiveservices.service.registry.client.RegistryListener;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class MapDbDomainEventsService
        implements Closeable, ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "mapdbdomainevents";
    public static final Marker MARKER = MarkerManager.getMarker("service:"+SERVICE_TYPE);

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server)
        {
            if (injectionManager.getInstance(Configuration.class).getConfiguration(SERVICE_TYPE).getBoolean("enabled", true))
            {
                server.addService(new ResourceObject.Builder("service", SERVICE_TYPE).build());
                MARKER.addParents(server.getServerLogMarker());

                context.register(MapDbDomainEventsService.class);
            }

            return super.configure(context, injectionManager, server);
        }
    }

    private final Logger logger = LogManager.getLogger(getClass());
    private final RegistryClient registryClient;
    private final MapDatabaseService mapDatabaseService;

    @Inject
    public MapDbDomainEventsService(RegistryClient registryClient, MapDatabaseService mapDatabaseService) {
        this.registryClient = registryClient;
        this.mapDatabaseService = mapDatabaseService;
    }

    @Override
    public void onStartup(Container container) {
        logger.info(MARKER, "Startup");

        registryClient.addRegistryListener(new DomainEventsRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }
    @Override
    public void onShutdown(Container container) {
        logger.info(MARKER, "Shutdown");
    }

    public void connect(Link domainEventSource) {
        Disruptor<EventHolder<JsonObject>> disruptor =
                new Disruptor<>(EventHolder::new, 4096, new NamedThreadFactory("MapDbDomainEventsDisruptorIn-"),
                        ProducerType.SINGLE,
                        new BlockingWaitStrategy());

        registryClient.connect(domainEventSource.getHrefAsUriTemplate().createURI(""), new DomainEventsClientEndpoint(disruptor))
                .thenAccept(session ->
                {
                    try {
                        disruptor.handleEventsWith(new MetadataDeserializerEventHandler(),
                                        new DomainEventDeserializeEventHandler())
                                .then(new MapDbDomainEventEventHandler(mapDatabaseService, session), new WebSocketFlowControlEventHandler(1, session, Executors.newSingleThreadExecutor()));
                        disruptor.start();

//                        logger.info(MARKER, "Connected to " + domainEventSource.getHref());
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }).exceptionally(e ->
                {
                    e.printStackTrace();
                    return null;
                });
    }

    @Override
    public void close() throws IOException {
        // TODO Close active sessions
    }

    private class DomainEventsRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("domainevents").ifPresent(MapDbDomainEventsService.this::connect);
        }
    }

    private static class DomainEventsClientEndpoint
            implements WebSocketListener {
        private final Logger logger = LogManager.getLogger(getClass());
        private final Disruptor<EventHolder<JsonObject>> disruptor;

        ByteBuffer headers;

        private DomainEventsClientEndpoint(
                Disruptor<EventHolder<JsonObject>> disruptor) {
            this.disruptor = disruptor;
        }

        @Override
        public void onWebSocketText(String message) {
            logger.debug(MARKER, "Text: {}",  message);
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int len) {
            if (headers == null) {
                headers = ByteBuffer.wrap(payload, offset, len);
            } else {
                ByteBuffer body = ByteBuffer.wrap(payload, offset, len);
                disruptor.publishEvent((holder, seq, h, b) ->
                {
                    holder.headers = h;
                    holder.body = b;
                }, headers, body);
                headers = null;
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            logger.info(MARKER, "Closed: {} ({})",  statusCode, reason);
            disruptor.shutdown();
        }

        @Override
        public void onWebSocketConnect(Session session) {
            logger.info(MARKER, "Connected to {}",  session.getUpgradeRequest().getRequestURI());
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            logger.error(MARKER, "Error",  cause);
        }
    }
}
