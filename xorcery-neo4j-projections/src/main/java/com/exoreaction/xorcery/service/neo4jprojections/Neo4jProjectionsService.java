package com.exoreaction.xorcery.service.neo4jprojections;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4jprojections.api.Neo4jProjectionStreams;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = Neo4jProjectionsService.SERVICE_TYPE)
@RunLevel(4)
public class Neo4jProjectionsService {

    public static final String SERVICE_TYPE = "neo4jprojections";

    private final Logger logger = LogManager.getLogger(getClass());
    private final ServiceResourceObject sro;
    private final Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher;
    private final GraphDatabase graphDatabase;

    @Inject
    public Neo4jProjectionsService(ServiceResourceObjects serviceResourceObjects,
                                   Configuration configuration,
                                   GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;

        Neo4jProjectionsConfiguration neo4jProjectionsConfiguration = new Neo4jProjectionsConfiguration(configuration.getConfiguration("neo4jprojections"));

        this.sro = new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), SERVICE_TYPE)
                .with(b -> {
                    if (neo4jProjectionsConfiguration.isEventSubscriberEnabled())
                        b.subscriber(Neo4jProjectionStreams.EVENT_SUBSCRIBER);
                    if (neo4jProjectionsConfiguration.isCommitPublisherEnabled())
                        b.publisher(Neo4jProjectionStreams.COMMIT_PUBLISHER);
                }).build();

        neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        serviceResourceObjects.add(sro);
    }

    public Optional<ProjectionModel> getCurrentProjection(String projectionId) {
        // Check if we already have written data for this projection before
        return graphDatabase.query("MATCH (Projection:Projection {id:$projection_id})")
                .parameter(Projection.id, projectionId)
                .results(Projection.version, Projection.revision)
                .first(row -> row.toModel(ProjectionModel::new, Projection.version, Projection.revision)).handle((model, exception) ->
                {
                    if (exception != null && !(exception.getCause() instanceof NotFoundException)) {
                        logger.error("Error looking up existing projection details", exception);
                        return Optional.<ProjectionModel>empty();
                    }
                    return Optional.ofNullable(model);
                }).toCompletableFuture().join();
    }

    public Neo4jProjectionCommitPublisher getNeo4jProjectionCommitPublisher() {
        return neo4jProjectionCommitPublisher;
    }
}
