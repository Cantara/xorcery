package com.exoreaction.reactiveservices.service.neo4j.client;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class GraphResult
    implements AutoCloseable
{
    private final Transaction tx;
    private final Result result;

    public GraphResult(Transaction tx, Result result) {

        this.tx = tx;
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public void close() throws Exception {
        result.close();
        tx.rollback();
    }
}
