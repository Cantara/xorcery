package com.exoreaction.xorcery.service.neo4j.client;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

public class GraphResult
    implements AutoCloseable
{
    private final Transaction tx;
    private final Result result;
    private AutoCloseable onClose;

    public GraphResult(Transaction tx, Result result, AutoCloseable onClose) {

        this.tx = tx;
        this.result = result;
        this.onClose = onClose;
    }

    public Result getResult() {
        return result;
    }

    @Override
    public void close() throws Exception {
        result.close();
        tx.rollback();

        onClose.close();
    }
}
