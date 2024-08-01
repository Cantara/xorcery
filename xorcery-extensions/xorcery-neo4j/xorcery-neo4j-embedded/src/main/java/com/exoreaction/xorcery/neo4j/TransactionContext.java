package com.exoreaction.xorcery.neo4j;

import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;

import java.util.HashMap;
import java.util.Optional;

/**
 * Makes it possible to associate arbitrary context with Neo4j transactions.
 */
public class TransactionContext {
    public static void setTransactionContext(Transaction tx, String name, Object value) {
        KernelTransaction kernelTransaction = ((InternalTransaction) tx).kernelTransaction();
        if (kernelTransaction.getMetaData() instanceof HashMap hashMap)
        {
            hashMap.put(name, Optional.of(value));
        } else
        {
            HashMap hashMap = new HashMap();
            kernelTransaction.setMetaData(hashMap);
            hashMap.put(name, Optional.of(value));
        }
    }

    public static <T> Optional<T> getTransactionContext(Transaction tx, String name) {
        KernelTransaction kernelTransaction = ((InternalTransaction)tx).kernelTransaction();
        return (Optional<T>)kernelTransaction.getMetaData().getOrDefault(name, Optional.<T>empty());
    }
}
