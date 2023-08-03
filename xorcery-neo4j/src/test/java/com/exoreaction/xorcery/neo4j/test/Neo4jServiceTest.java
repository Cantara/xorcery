package com.exoreaction.xorcery.neo4j.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.neo4j.Neo4jService;
import com.exoreaction.xorcery.neo4j.client.GraphResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Neo4jServiceTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void startupNeo4j() throws Exception {
        Neo4jService neo4jService = xorceryExtension.getXorcery().getServiceLocator().getService(Neo4jService.class);
        try (GraphResult graphResult = neo4jService.provide().execute("MATCH (n) RETURN count(n)", Collections.emptyMap(), 10).toCompletableFuture().join())
        {
            System.out.println(graphResult.getResult().resultAsString());
        }
    }
}
