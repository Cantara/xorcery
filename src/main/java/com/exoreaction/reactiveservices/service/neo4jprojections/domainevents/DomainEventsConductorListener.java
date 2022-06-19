package com.exoreaction.reactiveservices.service.neo4jprojections.domainevents;

import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.api.AbstractConductorListener;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabases;
import com.exoreaction.reactiveservices.service.neo4jprojections.Stream;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.NotFoundException;

import java.util.Optional;

public class DomainEventsConductorListener extends AbstractConductorListener {

    private GraphDatabases graphDatabases;
    private ReactiveStreams reactiveStreams;

    public DomainEventsConductorListener(GraphDatabases graphDatabases, ReactiveStreams reactiveStreams, ServiceIdentifier serviceIdentifier, String rel) {
        super(serviceIdentifier, rel);
        this.graphDatabases = graphDatabases;
        this.reactiveStreams = reactiveStreams;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Optional<ObjectNode> sourceAttributes, Optional<ObjectNode> consumerAttributes) {

        String databaseName = consumerAttributes.flatMap(sa -> new Attributes(sa).getOptionalString("database"))
                .orElse("neo4j");
        GraphDatabase graphDatabase = graphDatabases.apply(databaseName);

        // Check if we already have written data for this stream before
        String streamName = sourceAttributes.map(attrs -> attrs.path("stream")).map(JsonNode::textValue).orElseThrow();

        try {
            long position = graphDatabase.query("MATCH (stream:Stream {name:$stream_name})")
                    .parameter(Stream.name, streamName)
                    .results(Stream.revision)
                    .first(row -> row.row().getNumber("stream_revision").longValue()).toCompletableFuture().join();
            sourceAttributes.ifPresent(attrs -> attrs.set("from", attrs.numberNode(position)));
        } catch (NotFoundException e) {
            // No previous knowledge of this stream
        }

        reactiveStreams.subscribe(sro.serviceIdentifier(), link, new DomainEventsSubscriber(consumerAttributes, sourceAttributes, graphDatabase.getGraphDatabaseService()), sourceAttributes);
    }
}
