package com.exoreaction.xorcery.neo4j;

import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.ResourceMonitor;
import org.neo4j.kernel.impl.coreapi.InternalTransaction;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Makes it possible to associate arbitrary context with Neo4j transactions.
 * <p>
 * Access this in stored procedures/function using:
 * @Context Transaction tx
 * @Context ServiceLocator serviceLocator
 * and then: serviceLocator.getService(TransactionContext.class).getTransactionContext(tx,"myContextName")
 */
@Service
public class TransactionContext {

    private Map<KernelTransaction, Map<String, Object>> transactionContext = new ConcurrentHashMap<>();

    public void setTransactionContext(Transaction tx, String name, Object value) {
        KernelTransaction kernelTransaction = ((InternalTransaction)tx).kernelTransaction();
        Map<String, Object> transactionMap = transactionContext.get(kernelTransaction);
        if (transactionMap == null)
        {
            transactionMap = new ConcurrentHashMap<>();
            Map<String, Object> existingMap = transactionContext.put(kernelTransaction, transactionMap);
            if (existingMap == null)
            {
                ((ResourceMonitor)tx).registerCloseableResource(()->{
                    transactionContext.remove(kernelTransaction);
                });
            } else
            {
                transactionMap.putAll(existingMap);
            }
        }

        transactionMap.put(name, value);
    }

    public <T> Optional<T> getTransactionContext(Transaction tx, String name) {
        KernelTransaction kernelTransaction = ((InternalTransaction)tx).kernelTransaction();
        return transactionContext.get(kernelTransaction) instanceof ConcurrentHashMap<String, Object> txMap ? Optional.ofNullable((T)txMap.get(name)) : Optional.empty();
    }
}
