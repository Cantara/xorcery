package com.exoreaction.xorcery.binarytree.neo4j;

import no.cantara.binarytree.Node;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Neo4jNode implements Node {

    public static final String RELATION_LEFT = "LEFT";
    public static final String RELATION_RIGHT = "RIGHT";
    public static final String RELATION_PARENT = "PARENT";
    public static final String DATA_PROPERTY_KEY = "_key";
    public static final String PROPERTY_KEY_COLOR = "_color";
    public static final String PROPERTY_KEY_HEIGHT = "_height";

    public static final Set<String> NAVIGABLE_RELATIONS_SET = Set.of(RELATION_LEFT, RELATION_RIGHT, RELATION_PARENT);
    public static final Set<String> NAVIGABLE_PROPERTIES_SET = Set.of(DATA_PROPERTY_KEY, PROPERTY_KEY_COLOR, PROPERTY_KEY_HEIGHT);

    private static final RelationshipType RELATIONSHIP_TYPE_LEFT = RelationshipType.withName(RELATION_LEFT);
    private static final RelationshipType RELATIONSHIP_TYPE_RIGHT = RelationshipType.withName(RELATION_RIGHT);
    private static final RelationshipType RELATIONSHIP_TYPE_PARENT = RelationshipType.withName(RELATION_PARENT);

    private final org.neo4j.graphdb.Node neo4jNode;

    public Neo4jNode(org.neo4j.graphdb.Node neo4jNode) {
        this.neo4jNode = neo4jNode;
    }

    public org.neo4j.graphdb.Node neo4jNode() {
        return neo4jNode;
    }

    @Override
    public long data() {
        return (Long) neo4jNode.getProperty(DATA_PROPERTY_KEY);
    }

    @Override
    public Neo4jNode data(long data) {
        neo4jNode.setProperty(DATA_PROPERTY_KEY, data);
        return this;
    }

    @Override
    public Neo4jNode left() {
        Relationship left = neo4jNode.getSingleRelationship(RELATIONSHIP_TYPE_LEFT, Direction.OUTGOING);
        if (left == null) {
            return null;
        }
        org.neo4j.graphdb.Node leftNode = left.getEndNode();
        return new Neo4jNode(leftNode);
    }

    @Override
    public Neo4jNode left(Node left) {
        Relationship existingRelationship = neo4jNode.getSingleRelationship(RELATIONSHIP_TYPE_LEFT, Direction.OUTGOING);
        if (existingRelationship != null) {
            existingRelationship.delete();
        }
        if (left == null) {
            return this;
        }
        Neo4jNode leftNeo4j = (Neo4jNode) left;
        neo4jNode.createRelationshipTo(leftNeo4j.neo4jNode, RELATIONSHIP_TYPE_LEFT);
        return this;
    }

    @Override
    public Neo4jNode right() {
        Relationship right = neo4jNode.getSingleRelationship(RELATIONSHIP_TYPE_RIGHT, Direction.OUTGOING);
        if (right == null) {
            return null;
        }
        org.neo4j.graphdb.Node rightNode = right.getEndNode();
        return new Neo4jNode(rightNode);
    }

    @Override
    public Neo4jNode right(Node right) {
        Relationship existingRelationship = neo4jNode.getSingleRelationship(RELATIONSHIP_TYPE_RIGHT, Direction.OUTGOING);
        if (existingRelationship != null) {
            existingRelationship.delete();
        }
        if (right == null) {
            return this;
        }
        Neo4jNode rightNeo4j = (Neo4jNode) right;
        neo4jNode.createRelationshipTo(rightNeo4j.neo4jNode, RELATIONSHIP_TYPE_RIGHT);
        return this;
    }

    @Override
    public Neo4jNode parent() {
        Relationship parentRelationship = neo4jNode.getSingleRelationship(RELATIONSHIP_TYPE_PARENT, Direction.OUTGOING);
        if (parentRelationship == null) {
            return null;
        }
        return new Neo4jNode(parentRelationship.getEndNode());
    }

    @Override
    public Neo4jNode parent(Node parent) {
        Relationship existingParentRelationship = neo4jNode.getSingleRelationship(RELATIONSHIP_TYPE_PARENT, Direction.OUTGOING);
        if (existingParentRelationship != null) {
            existingParentRelationship.delete();
        }
        if (parent == null) {
            return this;
        }
        neo4jNode.createRelationshipTo(((Neo4jNode) parent).neo4jNode, RELATIONSHIP_TYPE_PARENT);
        return this;
    }

    @Override
    public int height() {
        return (Integer) neo4jNode.getProperty(PROPERTY_KEY_HEIGHT);
    }

    @Override
    public Neo4jNode height(int height) {
        neo4jNode.setProperty(PROPERTY_KEY_HEIGHT, height);
        return this;
    }

    @Override
    public boolean color() {
        return (Boolean) neo4jNode.getProperty(PROPERTY_KEY_COLOR);
    }

    @Override
    public Neo4jNode color(boolean color) {
        neo4jNode.setProperty(PROPERTY_KEY_COLOR, color);
        return this;
    }

    @Override
    public boolean isNil() {
        return false;
    }

    @Override
    public void delete() {
        try (ResourceIterable<Relationship> relationships = neo4jNode.getRelationships()) {
            for (Relationship relationship : relationships) {
                relationship.delete();
            }
        }
        neo4jNode.delete();
    }

    @Override
    public void copyNonNavigableStateFrom(Node _source) {
        Neo4jNode source = (Neo4jNode) _source;
        org.neo4j.graphdb.Node sourceNode = source.neo4jNode;
        // copy all outgoing relationships (except tree-navigation)
        try (ResourceIterable<Relationship> relationships = sourceNode.getRelationships(Direction.OUTGOING)) {
            for (Relationship relationship : relationships) {
                if (!NAVIGABLE_RELATIONS_SET.contains(relationship.getType().name())) {
                    neo4jNode.createRelationshipTo(relationship.getEndNode(), relationship.getType());
                }
            }
        }
        // copy all incoming relationships (except tree-navigation)
        try (ResourceIterable<Relationship> relationships = sourceNode.getRelationships(Direction.INCOMING)) {
            for (Relationship relationship : relationships) {
                if (!NAVIGABLE_RELATIONS_SET.contains(relationship.getType().name())) {
                    relationship.getStartNode().createRelationshipTo(neo4jNode, relationship.getType());
                }
            }
        }
        // copy all properties (except tree-navigation)
        for (Map.Entry<String, Object> entry : sourceNode.getAllProperties().entrySet()) {
            if (!NAVIGABLE_PROPERTIES_SET.contains(entry.getKey())) {
                neo4jNode.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Neo4jNode neo4jNode1)) return false;

        return Objects.equals(neo4jNode, neo4jNode1.neo4jNode);
    }

    @Override
    public int hashCode() {
        return neo4jNode != null ? neo4jNode.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Neo4jNodeId=" + neo4jNode.getElementId();
    }
}
