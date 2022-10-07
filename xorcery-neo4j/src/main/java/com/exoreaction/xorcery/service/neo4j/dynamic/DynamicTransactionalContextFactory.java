package com.exoreaction.xorcery.service.neo4j.dynamic;

import org.neo4j.kernel.api.query.ExecutingQuery;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.kernel.impl.query.TransactionalContext;
import org.neo4j.kernel.impl.query.TransactionalContextFactory;
import org.neo4j.values.virtual.MapValue;

public class DynamicTransactionalContextFactory
        implements TransactionalContextFactory
{
    private final TransactionalContextFactory delegate;

    public DynamicTransactionalContextFactory(TransactionalContextFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public TransactionalContext newContext(InternalTransaction tx, String queryText, MapValue queryParameters) {
        return delegate.newContext(tx, queryText, queryParameters);
    }

    @Override
    public TransactionalContext newContextForQuery(InternalTransaction tx, ExecutingQuery executingQuery) {
        return delegate.newContextForQuery(tx, executingQuery);
    }
}
