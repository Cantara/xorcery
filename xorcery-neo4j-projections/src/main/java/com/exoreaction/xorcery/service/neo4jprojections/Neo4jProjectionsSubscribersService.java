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
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.fasterxml.jackson.databind.node.ContainerNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = "neo4jprojections")
@RunLevel(8)
public class Neo4jProjectionsSubscribersService {

    private final Logger logger = LogManager.getLogger(getClass());
    private final GraphDatabase graphDatabase;

    @Inject
    public Neo4jProjectionsSubscribersService(ServiceResourceObjects serviceResourceObjects,
                                              Neo4jProjectionsService neo4jProjectionsService,
                                              ReactiveStreamsClient reactiveStreamsClient,
                                              Configuration configuration,
                                              GraphDatabases graphDatabases,
                                              IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                              MetricRegistry metricRegistry) {
        graphDatabase = graphDatabases.apply("neo4j");

        // This service can subscribe to external publishers
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
                                        neo4jProjectionsService.getNeo4jProjectionCommitPublisher(),
                                        projectionList,
                                        metricRegistry)), ProjectionSubscriber.class, Configuration.empty());
            }
        });
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
