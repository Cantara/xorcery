package com.exoreaction.xorcery.binarytree.neo4j;

import no.cantara.binarytree.NodeFactory;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class NodeFactorySingleton {

    private static final GraphDatabaseService graphDb;
    static volatile Transaction tx;

    static {
        String neo4jHomeStr = "target/_neo4j-bt-test-" + UUID.randomUUID();
        Path neo4jHomePath = Path.of(neo4jHomeStr);
        try {
            Files.createDirectories(neo4jHomePath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        DatabaseManagementService managementService = new DatabaseManagementServiceBuilder(neo4jHomePath).build();
        graphDb = managementService.database(DEFAULT_DATABASE_NAME);
        tx = graphDb.beginTx(3, TimeUnit.MINUTES);
    }

    public static NodeFactory instance() {
        tx.commit();
        tx.close();
        tx = null;
        tx = graphDb.beginTx(3, TimeUnit.MINUTES);
        Neo4jNodeFactory neo4jNodeFactory = new Neo4jNodeFactory(tx, Label.label("JUnit"));
        return neo4jNodeFactory;
    }
}
