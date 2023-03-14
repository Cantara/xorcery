package com.exoreaction.xorcery.service.neo4j.dynamic.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.neo4j.Neo4jService;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.Collections;

public class DynamicNeo4jTest {

    private static final String config = """
            neo4jdatabase:
                enabled: true
                path: "{{ instance.home }}/neo4j"
            """;

    private static final String neo4jSetup = """
            CREATE (node:Entity {tenant:'tenant1'})
            RETURN node
            """;

    private static final String queryTenantNode = """
            MATCH (node:Entity)
            RETURN node
            """;

    @Disabled
    @Test
    public void test() throws Exception {
        Configuration configuration = new Configuration.Builder().with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config)).build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            // Given
            GraphDatabase neo4j = xorcery.getServiceLocator().getService(Neo4jService.class).apply("neo4j");
            GraphDatabaseService graphDatabaseService = neo4j.getGraphDatabaseService();

            try (Transaction tx = graphDatabaseService.beginTx()) {
                for (String cypher : neo4jSetup.split(";")) {
                    tx.execute(cypher, Collections.emptyMap()).close();
                }
                tx.commit();
            }


            // When
            try (GraphResult result = neo4j.execute(queryTenantNode, Collections.emptyMap(), 0).toCompletableFuture().join()) {
                System.out.println(result.getResult().resultAsString());
            }

            // Then
        }
    }
}
