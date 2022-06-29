package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.Stream;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import jakarta.ws.rs.NotFoundException;

public class DomainEventsConductorListener extends AbstractConductorListener {

    private GraphDatabases graphDatabases;
    private ReactiveStreams reactiveStreams;

    public DomainEventsConductorListener(GraphDatabases graphDatabases, ReactiveStreams reactiveStreams, ServiceIdentifier serviceIdentifier, String rel) {
        super(serviceIdentifier, rel);
        this.graphDatabases = graphDatabases;
        this.reactiveStreams = reactiveStreams;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {

        String databaseName = consumerConfiguration.getString("database").orElse("src/main/resources/neo4j");
        GraphDatabase graphDatabase = graphDatabases.apply(databaseName);

        // Check if we already have written data for this stream before
        String streamName = sourceConfiguration.getString("stream").orElseThrow();

        try {
            long position = graphDatabase.query("MATCH (stream:Stream {name:$stream_name})")
                    .parameter(Stream.name, streamName)
                    .results(Stream.revision)
                    .first(row -> row.row().getNumber("stream_revision").longValue()).toCompletableFuture().join();
            sourceConfiguration.json().set("from", sourceConfiguration.json().numberNode(position));
        } catch (NotFoundException e) {
            // No previous knowledge of this stream
        }

        reactiveStreams.subscribe(sro.serviceIdentifier(), link, new DomainEventsSubscriber(consumerConfiguration, sourceConfiguration, graphDatabase.getGraphDatabaseService()), sourceConfiguration);
    }
}
