package com.exoreaction.xorcery.neo4j.providers;

import com.exoreaction.xorcery.neo4j.TransactionContext;
import com.exoreaction.xorcery.neo4j.spi.Neo4jProvider;
import org.glassfish.hk2.api.ServiceLocator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;

public class TransactionContextExtensions
    implements Neo4jProvider
{

    @Context
    public Transaction transaction;

    @Context
    public ServiceLocator serviceLocator;

    @Procedure("transaction.context")
    public void setTransactionContext(@Name("name") String name, @Name("value") Object value) {
        serviceLocator.getService(TransactionContext.class).setTransactionContext(transaction, name, value);
    }

    @UserFunction("transaction.context")
    public Object getTransactionContext(@Name("name") String name) {
        return serviceLocator.getService(TransactionContext.class).getTransactionContext(transaction, name).orElse(null);
    }
}
