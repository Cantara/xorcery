package com.exoreaction.reactiveservices.service.neo4jprojections.eventstore;

import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.api.AbstractConductorListener;
import com.exoreaction.reactiveservices.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabases;
import com.exoreaction.reactiveservices.service.neo4jprojections.ProjectionListener;
import com.exoreaction.reactiveservices.service.neo4jprojections.Stream;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.util.Listeners;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

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
    public void connect(ServiceResourceObject sro, Link link, Optional<ObjectNode> sourceAttributes, Optional<ObjectNode> consumerAttributes) {

        String databaseName = consumerAttributes.flatMap(sa -> new Attributes(sa).getOptionalString("database"))
                .orElse("neo4j");
        GraphDatabase graphDatabase = graphDatabases.apply(databaseName);

        // Check if we already have written data for this stream before
        try {
            EventStoreParameters parameters = new ObjectMapper().treeToValue(sourceAttributes.orElseThrow(), EventStoreParameters.class);

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
                            sourceAttributes.ifPresent(attrs -> attrs.set("from", attrs.numberNode(position)));
                        }

                        reactiveStreams.subscribe(sro.serviceIdentifier(), link, new EventStoreSubscriber(consumerAttributes, parameters, graphDatabase.getGraphDatabaseService(), listeners), sourceAttributes);
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
