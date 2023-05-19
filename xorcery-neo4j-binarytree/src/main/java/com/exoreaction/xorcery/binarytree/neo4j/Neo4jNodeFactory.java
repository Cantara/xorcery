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

import no.cantara.binarytree.Node;
import no.cantara.binarytree.NodeFactory;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;

public class Neo4jNodeFactory implements NodeFactory {

    private final Transaction tx;
    private final Label creationLabel;

    public Neo4jNodeFactory(Transaction tx, Label creationLabel) {
        this.tx = tx;
        this.creationLabel = creationLabel;
    }

    @Override
    public Neo4jNode createNode(long data) {
        org.neo4j.graphdb.Node node = tx.createNode(creationLabel);
        return new Neo4jNode(node)
                .data(data);
    }

    @Override
    public Node nilNode() {
        org.neo4j.graphdb.Node node = tx.createNode(creationLabel);
        return new Neo4jNilNode(node);
    }

    private static class Neo4jNilNode extends Neo4jNode {
        private Neo4jNilNode(org.neo4j.graphdb.Node neo4jNode) {
            super(neo4jNode);
        }

        @Override
        public boolean color() {
            return BLACK;
        }

        @Override
        public boolean isNil() {
            return true;
        }
    }
}
