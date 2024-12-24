/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.neo4j;

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
