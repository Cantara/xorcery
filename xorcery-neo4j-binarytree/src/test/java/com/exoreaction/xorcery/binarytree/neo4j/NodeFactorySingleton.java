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
