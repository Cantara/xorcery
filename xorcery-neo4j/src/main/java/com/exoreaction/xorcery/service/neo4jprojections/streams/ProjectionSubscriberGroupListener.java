package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.helpers.AbstractGroupListener;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ProjectionSubscriberGroupListener extends AbstractGroupListener {

    private final Logger logger = LogManager.getLogger(getClass());

    private final GraphDatabases graphDatabases;
    private final ReactiveStreams reactiveStreams;
    private List<Neo4jEventProjection> neo4jEventProjectionList;
    private final MetricRegistry metricRegistry;
    private Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher;

    public ProjectionSubscriberGroupListener(GraphDatabases graphDatabases,
                                             ReactiveStreams reactiveStreams,
                                             ServiceIdentifier serviceIdentifier,
                                             List<Neo4jEventProjection> neo4jEventProjectionList, MetricRegistry metricRegistry,
                                             Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher) {
        super(serviceIdentifier, null);
        this.graphDatabases = graphDatabases;
        this.reactiveStreams = reactiveStreams;
        this.neo4jEventProjectionList = neo4jEventProjectionList;
        this.metricRegistry = metricRegistry;
        this.neo4jProjectionCommitPublisher = neo4jProjectionCommitPublisher;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {

        String databaseName = consumerConfiguration.getString("database").orElse("neo4j");
        GraphDatabase graphDatabase = graphDatabases.apply(databaseName);

        // Check if we already have written data for this projection before
        String projectionId = consumerConfiguration.getString("id").orElse("default");

        graphDatabase.query("MATCH (Projection:Projection {id:$projection_id})")
                .parameter(Projection.id, projectionId)
                .results(Projection.revision)
                .first(row -> row.row().getNumber("projection_revision").longValue()).whenComplete((position, exception) ->
                {
                    if (exception != null && !(exception.getCause() instanceof NotFoundException)) {
                        logger.error("Error looking up existing projection stream revision", exception);
                    }

                    if (position != null) {
                        sourceConfiguration.json().set("from", sourceConfiguration.json().numberNode(position));
//                        neo4jProjectionCommitPublisher.accept();
                    }

                    reactiveStreams.subscribe(link.getHrefAsUri(),
                            sourceConfiguration,
                            new ProjectionSubscriber(
                                    subscription -> new Neo4jProjectionEventHandler(
                                            graphDatabase.getGraphDatabaseService(),
                                            subscription,
                                            sourceConfiguration.getLong("from"),
                                            consumerConfiguration,
                                            neo4jProjectionCommitPublisher,
                                            neo4jEventProjectionList,
                                            metricRegistry)),
                            ProjectionSubscriber.class);
                });
    }
}
