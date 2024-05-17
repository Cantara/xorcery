package com.exoreaction.xorcery.neo4j.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.neo4j.TransactionContext;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
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
    public void givenTransactionContextWhenGetTransactionContextThenTransactionContextIsReturned(GraphDatabase graphDatabase, TransactionContext transactionContext) throws Exception {

        try (Transaction tx = graphDatabase.getGraphDatabaseService().beginTx()) {
            transactionContext.setTransactionContext(tx, "foo", "bar1");

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