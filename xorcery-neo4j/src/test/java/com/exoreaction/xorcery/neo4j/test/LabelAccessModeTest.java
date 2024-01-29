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
package com.exoreaction.xorcery.neo4j.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.neo4j.Neo4jService;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4j.client.GraphResult;
import com.exoreaction.xorcery.neo4j.client.LabelAccessMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.graphdb.*;
import org.neo4j.internal.kernel.api.security.LoginContext;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.util.Collections;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LabelAccessModeTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void testLabelAccessMode() throws Exception {
        GraphDatabase graphDatabase = xorceryExtension.getServiceLocator().getService(GraphDatabase.class);
        GraphDatabaseAPI graphDatabaseService = (GraphDatabaseAPI) graphDatabase.getGraphDatabaseService();
        // Given two nodes with Entity label but different tenant labels
        Label sometenantid = Label.label("sometenantid");
        Label othertenantid = Label.label("othertenantid");
        try (Transaction tx = graphDatabaseService.beginTx()) {
            tx.createNode(sometenantid, Label.label("Entity"));
            tx.commit();
        }

        try (Transaction tx = graphDatabaseService.beginTx()) {
            tx.createNode(othertenantid, Label.label("Entity"));
            tx.commit();
        }

        // When accessing Entities through Java API or Cypher
        LoginContext loginContext = LabelAccessMode.getLoginContextForLabel("sometenantid");
        try (Transaction tx = graphDatabaseService.beginTransaction(KernelTransaction.Type.EXPLICIT, loginContext)) {
            try (ResourceIterator<Node> nodes = tx.findNodes(Label.label("Entity"))) {
                nodes.stream().forEach(n ->
                {
                    // Then only nodes with correct label is returned
                    Assertions.assertTrue(n.hasLabel(sometenantid));

                    System.out.print(n.getElementId() + ": ");
                    n.getLabels().forEach(System.out::print);
                    System.out.println();
                });
            }
        }

        try (Transaction tx = graphDatabaseService.beginTransaction(KernelTransaction.Type.EXPLICIT, loginContext)) {
            try (Result result = tx.execute("MATCH (n:Entity) RETURN n, labels(n)")) {
                // Then only nodes with correct label is returned
                while (result.hasNext()) {
                    Map<String, Object> row = result.next();
                    Node n = (Node) row.get("n");
                    Assertions.assertTrue(n.hasLabel(sometenantid));

                    System.out.println(row);
                }
            }
        }
    }
}
