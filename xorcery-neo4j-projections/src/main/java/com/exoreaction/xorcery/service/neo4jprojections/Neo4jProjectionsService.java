package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.api.Neo4jProjectionStreams;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.neo4jprojections.streams.*;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.fasterxml.jackson.databind.node.ContainerNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = Neo4jProjectionsService.SERVICE_TYPE)
public class Neo4jProjectionsService {

    public static final String SERVICE_TYPE = "neo4jprojections";

    private final Logger logger = LogManager.getLogger(getClass());
    private final ServiceResourceObject sro;
    private final GraphDatabase graphDatabase;

    @Inject
    public Neo4jProjectionsService(ServiceResourceObjects serviceResourceObjects,
                                   ReactiveStreamsServer reactiveStreamsServer,
                                   ReactiveStreamsClient reactiveStreamsClient,
                                   Configuration configuration,
                                   GraphDatabases graphDatabases,
                                   IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                   MetricRegistry metricRegistry) {
        this.sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .with(b -> {
                    if (configuration.getBoolean("neo4jprojections.eventsubscriber").orElse(true))
                        b.subscriber(Neo4jProjectionStreams.EVENT_SUBSCRIBER);
                    if (configuration.getBoolean("neo4jprojections.commitpublisher").orElse(true))
                        b.publisher(Neo4jProjectionStreams.COMMIT_PUBLISHER);
                }).build();

        graphDatabase = graphDatabases.apply("neo4j");

        Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        sro.getLinkByRel(Neo4jProjectionStreams.EVENT_SUBSCRIBER).ifPresent(link ->
        {
            List<Neo4jEventProjection> projectionList = new ArrayList<>();
            neo4jEventProjectionList.forEach(projectionList::add);

            reactiveStreamsServer.subscriber(link.getHrefAsUri().getPath(), cfg -> new ProjectionSubscriber(subscription -> new Neo4jProjectionEventHandler(
                    graphDatabases.apply(cfg.getString("database").orElse("neo4j")).getGraphDatabaseService(),
                    subscription,
                    getCurrentRevision(cfg.getString("projection").orElseThrow()),
                    cfg.getString("projection").orElseThrow(),
                    neo4jProjectionCommitPublisher,
                    projectionList,
                    metricRegistry)), ProjectionSubscriber.class);
        });

        sro.getLinkByRel(Neo4jProjectionStreams.COMMIT_PUBLISHER).ifPresent(link ->
        {
            reactiveStreamsServer.publisher(link.getHrefAsUri().getPath(), cfg -> neo4jProjectionCommitPublisher, Neo4jProjectionCommitPublisher.class);
        });

        // This service can subscribe to external publishers as well
        configuration.getObjectListAs("neo4jprojections.subscribers", Publisher::new).ifPresent(publishers ->
        {
            List<Neo4jEventProjection> projectionList = new ArrayList<>();
            neo4jEventProjectionList.forEach(projectionList::add);
            for (Publisher publisher : publishers) {

                final Configuration publisherConfiguration = publisher.getPublisherConfiguration().orElse(Configuration.empty());
                getCurrentRevision(publisher.getProjection()).ifPresent(revision -> publisherConfiguration.json().set("from", publisherConfiguration.json().numberNode(revision)));

                reactiveStreamsClient.subscribe(publisher.getAuthority(), publisher.getStream(),
                        ()->
                        {
                            // Update to current revision, if available
                            getCurrentRevision(publisher.getProjection()).ifPresent(revision -> publisherConfiguration.json().set("from", publisherConfiguration.json().numberNode(revision)));
                            return publisherConfiguration;
                        },
                        new ProjectionSubscriber(
                                subscription -> new Neo4jProjectionEventHandler(
                                        graphDatabase.getGraphDatabaseService(),
                                        subscription,
                                        publisherConfiguration.getLong("from"),
                                        publisher.getProjection(),
                                        neo4jProjectionCommitPublisher,
                                        projectionList,
                                        metricRegistry)), ProjectionSubscriber.class, Configuration.empty());
            }
        });

        serviceResourceObjects.add(sro);
    }

    private Optional<Long> getCurrentRevision(String projectionId) {
        return graphDatabase.query("MATCH (Projection:Projection {id:$projection_id})")
                .parameter(Projection.id, projectionId)
                .results(Projection.revision)
                .first(row -> row.row().getNumber("projection_revision").longValue()).handle((position, exception) ->
                {
                    if (exception != null && !(exception.getCause() instanceof NotFoundException)) {
                        LogManager.getLogger(getClass()).error("Error looking up existing projection stream revision", exception);
                    }

                    return Optional.ofNullable(position);
                }).toCompletableFuture().join();
    }

    record Publisher(ContainerNode<?> json)
            implements JsonElement {
        String getAuthority() {
            return getString("authority").orElseThrow();
        }

        String getStream() {
            return getString("stream").orElseThrow();
        }

        String getProjection() {
            return getString("projection").orElseThrow();
        }

        Optional<Configuration> getPublisherConfiguration() {
            return getObjectAs("configuration", Configuration::new);
        }
    }
}
