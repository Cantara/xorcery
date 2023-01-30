package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
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

@Service(name = "neo4jprojections.eventsubscriber")
@RunLevel(6)
public class Neo4jProjectionsSubscriberService {

    private final Logger logger = LogManager.getLogger(getClass());
    private final GraphDatabase graphDatabase;

    @Inject
    public Neo4jProjectionsSubscriberService(ReactiveStreamsServer reactiveStreamsServer,
                                             Neo4jProjectionsService neo4jProjectionsService,
                                             Configuration configuration,
                                             GraphDatabases graphDatabases,
                                             IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                             MetricRegistry metricRegistry) {
        graphDatabase = graphDatabases.apply("neo4j");

        Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        List<Neo4jEventProjection> projectionList = new ArrayList<>();
        neo4jEventProjectionList.forEach(projectionList::add);

        reactiveStreamsServer.subscriber("neo4jprojections", cfg -> new ProjectionSubscriber(subscription -> new Neo4jProjectionEventHandler(
                graphDatabases.apply(cfg.getString("database").orElse("neo4j")).getGraphDatabaseService(),
                subscription,
                getCurrentRevision(cfg.getString("projection").orElseThrow()),
                cfg.getString("projection").orElseThrow(),
                neo4jProjectionCommitPublisher,
                projectionList,
                metricRegistry)), ProjectionSubscriber.class);

        if (configuration.getBoolean("neo4jprojections.commitpublisher.enabled").orElse(true))
        {
            reactiveStreamsServer.publisher("neo4jprojectioncommits", cfg -> neo4jProjectionsService.getNeo4jProjectionCommitPublisher(), Neo4jProjectionCommitPublisher.class);
        }
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
}
