package dev.xorcery.neo4j.client;


import org.neo4j.graphdb.*;

import java.util.Optional;
import java.util.stream.Stream;

public interface NodeElement
        extends EntityElement {
    Node node();

    default Stream<Node> getRelatedNodes(Direction dir, RelationshipType relationshipType) {
        return node().getRelationships(dir, relationshipType)
                .stream()
                .map(dir == Direction.OUTGOING ? Relationship::getEndNode : Relationship::getStartNode);
    }

    default Optional<Node> getRelatedNode(Direction dir, RelationshipType relationshipType) {
        try (Stream<Node> nodes = getRelatedNodes(dir, relationshipType)) {
            return nodes.findFirst();
        }
    }

    @Override
    default Entity entity() {
        return node();
    }
}
