package com.exoreaction.xorcery.service.neo4j.dynamic;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.ResourceTracker;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.availability.DatabaseAvailabilityGuard;
import org.neo4j.kernel.impl.coreapi.TransactionExceptionMapper;
import org.neo4j.kernel.impl.coreapi.TransactionImpl;
import org.neo4j.kernel.impl.query.QueryExecutionEngine;
import org.neo4j.kernel.impl.query.TransactionalContextFactory;
import org.neo4j.token.TokenHolders;
import org.neo4j.values.ElementIdMapper;

import java.util.function.Consumer;

public class CustomTransactionImpl
    extends TransactionImpl
{
    public CustomTransactionImpl(TokenHolders tokenHolders, TransactionalContextFactory contextFactory, DatabaseAvailabilityGuard availabilityGuard, QueryExecutionEngine executionEngine, KernelTransaction transaction, ElementIdMapper elementIdMapper) {
        super(tokenHolders, contextFactory, availabilityGuard, executionEngine, transaction, elementIdMapper);
    }

    public CustomTransactionImpl(TokenHolders tokenHolders, TransactionalContextFactory contextFactory, DatabaseAvailabilityGuard availabilityGuard, QueryExecutionEngine executionEngine, KernelTransaction transaction, ResourceTracker coreApiResourceTracker, Consumer<Status> terminationCallback, TransactionExceptionMapper exceptionMapper, ElementIdMapper elementIdMapper) {
        super(tokenHolders, contextFactory, availabilityGuard, executionEngine, transaction, coreApiResourceTracker, terminationCallback, exceptionMapper, elementIdMapper);
    }

    @Override
    public ResourceIterator<Node> findNodes(Label myLabel) {
        return super.findNodes(myLabel);
    }
}
