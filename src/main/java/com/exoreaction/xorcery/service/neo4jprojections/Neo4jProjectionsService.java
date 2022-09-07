package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.jaxrs.readers.JsonApiMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams2;
import com.exoreaction.xorcery.util.Listeners;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.domainevents.DomainEventsConductorListener;
import com.exoreaction.xorcery.service.neo4jprojections.eventstore.EventStoreConductorListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
@Contract
public class Neo4jProjectionsService
        implements ContainerLifecycleListener, Neo4jProjections {

    public static final String SERVICE_TYPE = "neo4jprojections";

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {

        }

        @Override
        protected void configure() {
            context.register(Neo4jProjectionsService.class, ContainerLifecycleListener.class, Neo4jProjections.class);
        }
    }

    private final Logger logger = LogManager.getLogger(getClass());
    private MetricRegistry metricRegistry;
    private final JsonApiClient jsonApiClient;
    private final ReactiveStreams2 reactiveStreams;
    private final Conductor conductor;
    private final ServiceResourceObject sro;
    private final GraphDatabases graphDatabases;
    private final Map<String, CompletableFuture<Void>> isLive = new ConcurrentHashMap<>();

    private final Listeners<ProjectionListener> listeners = new Listeners<>(ProjectionListener.class);

    @Inject
    public Neo4jProjectionsService(Conductor conductor,
                                   ReactiveStreams2 reactiveStreams,
                                   @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                   GraphDatabases graphDatabases,
                                   MetricRegistry metricRegistry,
                                   JettyHttpClientContract clientInstance) {
        this.conductor = conductor;
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;
        this.graphDatabases = graphDatabases;
        this.metricRegistry = metricRegistry;

        this.jsonApiClient = new JsonApiClient(ClientBuilder.newBuilder().withConfig(new ClientConfig()
                .register(new JsonApiMessageBodyReader(new ObjectMapper()))
                .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger("client.neo4jprojections")).build())
                .register(clientInstance)
                .connectorProvider(new JettyConnectorProvider()))
                .build());
    }

    @Override
    public void onStartup(Container container) {
        conductor.addConductorListener(new DomainEventsConductorListener(graphDatabases, reactiveStreams, sro.serviceIdentifier(), "domainevents", metricRegistry, listeners, this::isLive));
        conductor.addConductorListener(new EventStoreConductorListener(graphDatabases, reactiveStreams, jsonApiClient, sro.serviceIdentifier(), "events", listeners, this::isLive));
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public void addProjectionListener(ProjectionListener listener) {
        listeners.addListener(listener);
    }

    public CompletableFuture<Void> isLive(String projectionId)
    {
        return isLive.computeIfAbsent(projectionId, id -> new CompletableFuture<>());
    }
}
