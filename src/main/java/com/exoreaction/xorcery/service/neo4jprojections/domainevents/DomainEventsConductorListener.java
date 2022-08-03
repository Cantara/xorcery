package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.xorcery.util.Listeners;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class DomainEventsConductorListener extends AbstractConductorListener {

    private Logger logger = LogManager.getLogger(getClass());

    private GraphDatabases graphDatabases;
    private ReactiveStreams reactiveStreams;
    private Listeners<ProjectionListener> listeners;
    private Function<String, CompletableFuture<Void>> isLive;

    public DomainEventsConductorListener(GraphDatabases graphDatabases,
                                         ReactiveStreams reactiveStreams,
                                         ServiceIdentifier serviceIdentifier,
                                         String rel,
                                         Listeners<ProjectionListener> listeners,
                                         Function<String, CompletableFuture<Void>> isLive) {
        super(serviceIdentifier, rel);
        this.graphDatabases = graphDatabases;
        this.reactiveStreams = reactiveStreams;
        this.listeners = listeners;
        this.isLive = isLive;
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
                    }

                    reactiveStreams.subscribe(sro.serviceIdentifier(), link,
                            new DomainEventsSubscriber(consumerConfiguration,
                                    sourceConfiguration,
                                    graphDatabase.getGraphDatabaseService(),
                                    listeners), sourceConfiguration);

                    // No catchup, we're live
                    isLive.apply(projectionId).complete(null);
                });
    }
}
