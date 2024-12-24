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
package dev.xorcery.neo4j.providers;

import dev.xorcery.neo4j.TransactionContext;
import dev.xorcery.neo4j.spi.Neo4jProvider;
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

    @Procedure("transaction.context")
    public void setTransactionContext(@Name("name") String name, @Name("value") Object value) {
        TransactionContext.setTransactionContext(transaction, name, value);
    }

    @UserFunction("transaction.context")
    public Object getTransactionContext(@Name("name") String name) {
        return TransactionContext.getTransactionContext(transaction, name).orElse(null);
    }
}
