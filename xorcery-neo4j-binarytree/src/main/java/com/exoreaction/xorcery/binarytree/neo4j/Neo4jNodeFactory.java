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
