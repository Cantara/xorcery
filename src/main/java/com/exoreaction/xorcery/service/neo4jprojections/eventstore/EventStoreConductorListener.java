package com.exoreaction.xorcery.service.neo4jprojections.eventstore;

import com.exoreaction.xorcery.util.Listeners;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.neo4jprojections.Stream;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventStoreConductorListener extends AbstractConductorListener {

    private Logger logger = LogManager.getLogger(getClass());

    private GraphDatabases graphDatabases;
    private ReactiveStreams reactiveStreams;
    private Listeners<ProjectionListener> listeners;

    public EventStoreConductorListener(GraphDatabases graphDatabases, ReactiveStreams reactiveStreams, ServiceIdentifier serviceIdentifier, String rel, Listeners<ProjectionListener> listeners) {
        super(serviceIdentifier, rel);
        this.graphDatabases = graphDatabases;
        this.reactiveStreams = reactiveStreams;
        this.listeners = listeners;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {

        String databaseName = consumerConfiguration.getString("database").orElse("neo4j");
        GraphDatabase graphDatabase = graphDatabases.apply(databaseName);

        // Check if we already have written data for this stream before
        try {
            EventStoreParameters parameters = new ObjectMapper().treeToValue(sourceConfiguration.json(), EventStoreParameters.class);

            graphDatabase.query("MATCH (Stream:Stream {name:$stream_name})")
                    .parameter(Stream.name, parameters.stream)
                    .results(Stream.revision)
                    .first(row -> row.row().getNumber("stream_revision").longValue()).whenComplete((position, exception) ->
                    {
                        if (exception != null && !(exception.getCause() instanceof NotFoundException))
                        {
                            logger.error("Error looking up existing stream position", exception);
                        }

                        if (position != null) {
                            sourceConfiguration.json().set("from", sourceConfiguration.json().numberNode(position));
                        }

                        reactiveStreams.subscribe(sro.serviceIdentifier(), link, new EventStoreSubscriber(consumerConfiguration, parameters, graphDatabase.getGraphDatabaseService(), listeners), sourceConfiguration);
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
