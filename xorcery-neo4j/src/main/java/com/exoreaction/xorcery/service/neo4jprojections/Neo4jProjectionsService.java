package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.jersey.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.api.Neo4jProjectionRels;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
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
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

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
    private ServiceLocator serviceLocator;
    private final ReactiveStreams reactiveStreams;
    private final GraphDatabases graphDatabases;
    private final MetricRegistry metricRegistry;
    private final ServiceResourceObject sro;

    @Inject
    public Neo4jProjectionsService(Conductor conductor,
                                   ServiceLocator serviceLocator,
                                   ReactiveStreams reactiveStreams,
                                   @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                   GraphDatabases graphDatabases,
                                   MetricRegistry metricRegistry) {
        this.conductor = conductor;
        this.serviceLocator = serviceLocator;
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;
        this.graphDatabases = graphDatabases;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onStartup(Container container) {

        List<Neo4jEventProjection> neo4jEventProjectionList = ServiceLoader.load(Neo4jEventProjection.class)
                .stream()
                .map(p -> serviceLocator.create(p.type()))
                .map(Neo4jEventProjection.class::cast)
                .toList();

        Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        conductor.addConductorListener(new ProjectionSubscriberConductorListener(graphDatabases,
                reactiveStreams, sro.serviceIdentifier(), neo4jEventProjectionList, metricRegistry, neo4jProjectionCommitPublisher));

        sro.getLinkByRel(Neo4jProjectionRels.neo4jprojectionsubscriber.name()).ifPresent(link ->
        {
            reactiveStreams.subscriber(link.getHrefAsUri().getPath(), cfg -> new ProjectionSubscriber(subscription -> new Neo4jProjectionEventHandler(
                    graphDatabases.apply(cfg.getString("database").orElse("neo4j")).getGraphDatabaseService(),
                    subscription,
                    Optional.empty(),
                    cfg,
                    neo4jProjectionCommitPublisher,
                    neo4jEventProjectionList,
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
