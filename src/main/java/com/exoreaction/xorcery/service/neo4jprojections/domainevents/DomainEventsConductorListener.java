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

public class DomainEventsConductorListener extends AbstractConductorListener {

    private Logger logger = LogManager.getLogger(getClass());

    private GraphDatabases graphDatabases;
    private ReactiveStreams reactiveStreams;
    private Listeners<ProjectionListener> listeners;

    public DomainEventsConductorListener(GraphDatabases graphDatabases,
                                         ReactiveStreams reactiveStreams,
                                         ServiceIdentifier serviceIdentifier,
                                         String rel,
                                         Listeners<ProjectionListener> listeners) {
        super(serviceIdentifier, rel);
        this.graphDatabases = graphDatabases;
        this.reactiveStreams = reactiveStreams;
        this.listeners = listeners;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {

        String databaseName = consumerConfiguration.getString("database").orElse("neo4j");
        GraphDatabase graphDatabase = graphDatabases.apply(databaseName);

        // Check if we already have written data for this projection before
        String streamName = sourceConfiguration.getString("stream").orElseThrow(() -> new IllegalStateException("Missing 'stream' configuration setting"));

        graphDatabase.query("MATCH (Projection:Projection {name:$projection_name})")
                .parameter(Projection.name, streamName)
                .results(Projection.revision)
                .first(row -> row.row().getNumber("projection_revision").longValue()).whenComplete((position, exception) ->
                {
                    if (exception != null && !(exception.getCause() instanceof NotFoundException)) {
                        logger.error("Error looking up existing projection stream position", exception);
                    }

                    if (position != null) {
                        sourceConfiguration.json().set("from", sourceConfiguration.json().numberNode(position));
                    }

                    reactiveStreams.subscribe(sro.serviceIdentifier(), link,
                            new DomainEventsSubscriber(consumerConfiguration,
                                    sourceConfiguration,
                                    graphDatabase.getGraphDatabaseService(),
                                    listeners), sourceConfiguration);
                });
    }
}
