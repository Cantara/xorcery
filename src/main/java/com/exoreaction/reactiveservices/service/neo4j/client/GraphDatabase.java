package com.exoreaction.reactiveservices.service.neo4j.client;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

public record GraphDatabase(GraphDatabaseService graphDatabaseService) {
    public CompletionStage<GraphResult> execute(String cypherQuery, Map<String, Object> parameters) {

        CompletableFuture<GraphResult> future = new CompletableFuture<>();

        ForkJoinPool.commonPool().execute(() ->
        {
            Transaction tx = graphDatabaseService.beginTx();
            try {
                Result result = tx.execute(cypherQuery, parameters);
                future.complete(new GraphResult(tx, result));
            } catch (QueryExecutionException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
