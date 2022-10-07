package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.jersey.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.api.Neo4jProjectionRels;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriberConductorListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.Optional;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
@Contract
public class Neo4jProjectionsService
        implements ContainerLifecycleListener {

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
            builder.websocket(Neo4jProjectionRels.neo4jprojectionsubscriber.name(), "ws/neo4jprojectionsubscriber")
                    .websocket(Neo4jProjectionRels.neo4jprojectioncommits.name(), "ws/neo4jprojectioncommits");
        }

        @Override
        protected void configure() {
            context.register(Neo4jProjectionsService.class, ContainerLifecycleListener.class);
        }
    }

    private final Logger logger = LogManager.getLogger(getClass());
    private final Conductor conductor;
    private final ReactiveStreams reactiveStreams;
    private final GraphDatabases graphDatabases;
    private final MetricRegistry metricRegistry;
    private final ServiceResourceObject sro;

    @Inject
    public Neo4jProjectionsService(Conductor conductor,
                                   ReactiveStreams reactiveStreams,
                                   @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                   GraphDatabases graphDatabases,
                                   MetricRegistry metricRegistry) {
        this.conductor = conductor;
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;
        this.graphDatabases = graphDatabases;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onStartup(Container container) {

        Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        conductor.addConductorListener(new ProjectionSubscriberConductorListener(graphDatabases,
                reactiveStreams, sro.serviceIdentifier(), metricRegistry, neo4jProjectionCommitPublisher));

        sro.getLinkByRel(Neo4jProjectionRels.neo4jprojectionsubscriber.name()).ifPresent(link ->
        {
            reactiveStreams.subscriber(link.getHrefAsUri().getPath(), cfg -> new ProjectionSubscriber(subscription -> new Neo4jProjectionEventHandler(
                    graphDatabases.apply(cfg.getString("database").orElse("neo4j")).getGraphDatabaseService(),
                    subscription,
                    Optional.empty(),
                    cfg,
                    neo4jProjectionCommitPublisher,
                    metricRegistry)), ProjectionSubscriber.class);
        });

        sro.getLinkByRel(Neo4jProjectionRels.neo4jprojectioncommits.name()).ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> neo4jProjectionCommitPublisher, Neo4jProjectionCommitPublisher.class);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }
}
