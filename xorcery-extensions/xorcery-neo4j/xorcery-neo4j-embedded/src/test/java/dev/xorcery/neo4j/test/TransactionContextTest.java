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
package dev.xorcery.neo4j.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.neo4j.TransactionContext;
import dev.xorcery.neo4j.client.GraphDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionContextTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    neo4jdatabase:
                      settings:
                        dbms:
                          security:
                            procedures:
                              unrestricted: "transaction.*"
                                          """)
            .build();

    @Test
    public void givenTransactionContextWhenGetTransactionContextThenTransactionContextIsReturned(GraphDatabase graphDatabase) throws Exception {

        try (Transaction tx = graphDatabase.getGraphDatabaseService().beginTx()) {
            TransactionContext.setTransactionContext(tx, "foo", "bar1");

            try (Result graphResult = tx.execute("""
                    UNWIND transaction.context("foo") as foo RETURN foo
                    """)) {
                System.out.println(graphResult.resultAsString());
            }

            tx.execute("""
                    CALL transaction.context("foo","bar2")
                    """).close();

            try (Result graphResult = tx.execute("""
                    UNWIND transaction.context("foo") as foo RETURN foo
                    """)) {
                System.out.println(graphResult.resultAsString());
            }
        }
    }
}