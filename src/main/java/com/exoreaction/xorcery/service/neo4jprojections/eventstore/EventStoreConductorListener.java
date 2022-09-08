package com.exoreaction.xorcery.service.neo4jprojections.eventstore;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.rest.RestProcess;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams2;
import com.exoreaction.xorcery.util.Listeners;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class EventStoreConductorListener extends AbstractConductorListener {

    private Logger logger = LogManager.getLogger(getClass());

    private GraphDatabases graphDatabases;
    private ReactiveStreams2 reactiveStreams;
    private JsonApiClient client;
    private Listeners<ProjectionListener> listeners;
    private Function<String, CompletableFuture<Void>> isLive;

    public EventStoreConductorListener(GraphDatabases graphDatabases,
                                       ReactiveStreams2 reactiveStreams,
                                       JsonApiClient client,
                                       ServiceIdentifier serviceIdentifier,
                                       String rel,
                                       Listeners<ProjectionListener> listeners,
                                       Function<String, CompletableFuture<Void>> isLive) {
        super(serviceIdentifier, rel);
        this.graphDatabases = graphDatabases;
        this.reactiveStreams = reactiveStreams;
        this.client = client;
        this.listeners = listeners;
        this.isLive = isLive;
    }

    @Override
    public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {

        String databaseName = consumerConfiguration.getString("database").orElse("neo4j");
        GraphDatabase graphDatabase = graphDatabases.apply(databaseName);

        // Check if we already have written data for this stream before
        try {
            EventStoreParameters parameters = new ObjectMapper().treeToValue(sourceConfiguration.json(), EventStoreParameters.class);

            // Get stream info
            sro.getLinkByRel("eventstore")
                    .map(l ->
                    {
                        EventStoreSubscription eventStoreSubscription = new EventStoreSubscription(client, l, parameters.stream, new CompletableFuture<>());
                        eventStoreSubscription.start();
                        return eventStoreSubscription.result();
                    })
                    .ifPresent(revision ->
                    {
                        long lastRevision;
                        try {
                            lastRevision = revision.toCompletableFuture().get(10, TimeUnit.SECONDS);
                            logger.info("Last revision for stream "+parameters.stream+" is "+lastRevision);
                        } catch (Throwable e) {
                            logger.error("Could not get last revision for stream "+parameters.stream);
                            lastRevision = -1;
                        }

                        String projectionId = consumerConfiguration.getString("id").orElse("default");

                        long finalLastRevision = lastRevision;
                        graphDatabase.query("MATCH (Projection:Projection {id:$projection_id})")
                                .parameter(Projection.id, projectionId)
                                .results(Projection.revision)
                                .first(row -> row.row().getNumber("projection_revision").longValue()).whenComplete((position, exception) ->
                                {
                                    if (exception != null && !(exception.getCause() instanceof NotFoundException))
                                    {
                                        logger.error("Error looking up existing projection stream revision", exception);
                                    }

                                    if (position != null) {
                                        sourceConfiguration.json().set("from", sourceConfiguration.json().numberNode(position));

                                        if (finalLastRevision != -1 && finalLastRevision == position)
                                        {
                                            // We're live
                                            logger.info("Projection "+projectionId+" is now live");
                                            isLive.apply(projectionId).complete(null);
                                        }
                                    }

                                    reactiveStreams.subscribe(link.getHrefAsUri(),
                                            sourceConfiguration,
                                            new EventStoreSubscriber(consumerConfiguration, parameters, graphDatabase.getGraphDatabaseService(), listeners, finalLastRevision, isLive.apply(projectionId)), EventStoreSubscriber.class);
                                });
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public record EventStoreSubscription(JsonApiClient client, Link eventStoreApi, String streamId, CompletionStage<Long> result)
        implements RestProcess<Long>
    {
        @Override
        public void start() {
            client.get(eventStoreApi)
                    .thenCompose(api ->
                    {
                        return client.get(api.getLinks().getByRel("stream")
                                .orElseThrow(()->new IllegalStateException("No stream link found"))
                                .createURI(streamId));
                    }).thenApply(stream->
                    {
                        return stream.getResource().orElseThrow(()->new IllegalStateException("No stream resource found"))
                                .getAttributes().getLong("revision")
                                .orElseThrow(()-> new IllegalStateException("No stream revision found"));
                    })
                    .whenComplete(this::complete);
        }
    }
}
