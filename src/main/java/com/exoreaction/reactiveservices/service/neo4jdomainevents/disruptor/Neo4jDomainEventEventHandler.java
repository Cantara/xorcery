package com.exoreaction.reactiveservices.service.neo4jdomainevents.disruptor;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.internal.Files;
import jakarta.json.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jDomainEventEventHandler
    implements DefaultEventHandler<Event<EventWithResult<JsonArray, Metadata>>>
{
    private ReactiveEventStreams.Subscription subscription;
    long version = 0;
    private GraphDatabaseService graphDatabaseService;

    private Transaction tx;
    private List<CompletableFuture<Metadata>> futures = new ArrayList<>();

    public Neo4jDomainEventEventHandler(GraphDatabaseService graphDatabaseService,
                                        ReactiveEventStreams.Subscription subscription) {
        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
    }

    @Override
    public void onEvent(Event<EventWithResult<JsonArray, Metadata>> event, long sequence, boolean endOfBatch ) throws Exception
    {
        JsonArray eventsJson = event.event.event();

        eventsJson.stream().map(JsonObject.class::cast).forEach( jsonObject ->
        {
            String type = jsonObject.getString("eventtype");

            Map<String, Object> parameters = jsonObject.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                    switch (entry.getValue().getValueType()) {
                        case ARRAY -> null;
                        case OBJECT -> null;
                        case STRING -> ((JsonString) entry.getValue()).getString();
                        case NUMBER -> ((JsonNumber)entry.getValue()).numberValue();
                        case TRUE -> Boolean.TRUE;
                        case FALSE -> Boolean.FALSE;
                        case NULL -> null;
                    }));

            try {
                String statement = Files.read(getClass().getResourceAsStream("/neo4j/"+type+".cyp"), StandardCharsets.UTF_8);

                graphDatabaseService.executeTransactionally(statement, parameters);
            } catch (IOException e) {
                event.event.result().completeExceptionally(e);
            }
        });

        futures.add(event.event.result());

        if (endOfBatch)
        {
            try {
                tx.commit();
                tx.close();
                tx = graphDatabaseService.beginTx();

                for (CompletableFuture<Metadata> future : futures) {
                    version++;
                    Metadata result = new Metadata();
                    result.add("version", Long.toString(version));
                    future.complete(result);
                }
            } catch (Exception e) {
                for (CompletableFuture<Metadata> future : futures) {
                    future.completeExceptionally(e);
                }
            }
            futures.clear();
        }

        subscription.request(1);
    }

    @Override
    public void onStart() {
        tx = graphDatabaseService.beginTx();
    }

    @Override
    public void onShutdown() {
        tx.commit();
        tx.close();
    }
}
