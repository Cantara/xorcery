package com.exoreaction.reactiveservices.service.neo4j.client;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class GraphDatabase {

    private final GraphDatabaseService graphDatabaseService;
    private Function<Enum<?>, String> defaultFieldMapping;

    public GraphDatabase(GraphDatabaseService graphDatabaseService,
                         Function<Enum<?>,String> defaultFieldMapping) {
        this.graphDatabaseService = graphDatabaseService;
        this.defaultFieldMapping = defaultFieldMapping;
    }

    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    public GraphQuery query(String baseQuery)
    {
        return new GraphQuery( baseQuery, defaultFieldMapping, q ->
        {
            String queryString = q.build(  );

            Map<String,Object> fixedParams = new HashMap<String,Object>( q.getParameters().size() );
            for ( Map.Entry<Enum<?>,Object> param : q.getParameters().entrySet() )
            {
                fixedParams.put( Cypher.toField( param.getKey() ), param.getValue() );
            }

            return execute( queryString, fixedParams, q.getTimeout() );
        } );
    }

    public CompletionStage<GraphResult> execute(String cypherQuery, Map<String, Object> parameters, long timeout) {

        CompletableFuture<GraphResult> future = new CompletableFuture<>();

        ForkJoinPool.commonPool().execute(() ->
        {
            Transaction tx = graphDatabaseService.beginTx(timeout, TimeUnit.SECONDS);
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
