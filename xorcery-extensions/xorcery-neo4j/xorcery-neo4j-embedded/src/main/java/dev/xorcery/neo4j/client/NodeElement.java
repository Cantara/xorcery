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
package dev.xorcery.neo4j.client;


import org.neo4j.graphdb.*;
import org.neo4j.kernel.impl.core.NodeEntity;

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

    @Override
    default Transaction getTransaction() {
        return ((NodeEntity)node()).getTransaction();
    }
}
