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
package com.exoreaction.xorcery.domainevents.neo4jprojections;

import com.exoreaction.xorcery.domainevents.api.DomainEvent;
import com.exoreaction.xorcery.domainevents.api.JsonDomainEvent;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.apache.logging.log4j.LogManager;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.*;

import java.util.Iterator;
import java.util.Map;

@Service(name = "jsondomaineventprojection")
@ContractsProvided({Neo4jEventProjection.class})
public class JsonDomainEventNeo4jEventProjection
        implements Neo4jEventProjection {

    public static final Label ENTITY_LABEL = Label.label("Entity");
    public static final Label AGGREGATE_LABEL = Label.label("Aggregate");

    @Override
    public void write(MetadataEvents events, Transaction transaction) throws Throwable {
        Map<String, Object> metadataMap = Cypher.toMap(events.metadata().metadata());

        for (DomainEvent domainEvent : events.data()) {
            if (domainEvent instanceof JsonDomainEvent jsonDomainEvent)
            {

            Neo4jJsonDomainEvent neo4jJsonDomainEvent = new Neo4jJsonDomainEvent(jsonDomainEvent);
            Object timestamp = metadataMap.get("timestamp");
            Object aggregateId = metadataMap.get("aggregateId");
            String aggregateType = metadataMap.get("aggregateType").toString();
            if (jsonDomainEvent.isCreated()) {
                JsonDomainEvent.JsonEntity entity = jsonDomainEvent.getEntity();
                Node node = transaction.createNode(Label.label(entity.getType()), ENTITY_LABEL);

                for (String label : neo4jJsonDomainEvent.labels()) {
                    node.addLabel(Label.label(label));
                }

                node.setProperty("createdOn", timestamp);
                node.setProperty("lastUpdatedOn", timestamp);
                String id = entity.getId();
                node.setProperty("id", id);
                if (aggregateId != null) {
                    node.setProperty("aggregate_id", aggregateId);
                }

                attributes(jsonDomainEvent.json(), node);
                addedRelationships(jsonDomainEvent.json(), transaction, node);
                removedRelationships(jsonDomainEvent.json(), node);

                // Create/update aggregate node
                if (aggregateType != null && aggregateId != null) {
                    Node aggregateNode = null;
                    if (!id.equals(aggregateId)) {
                        aggregateNode = transaction.findNode(AGGREGATE_LABEL, "id", aggregateId);
                    }

                    if (aggregateNode == null) {
                        aggregateNode = transaction.createNode(Label.label(aggregateType), AGGREGATE_LABEL);
                        aggregateNode.setProperty("createdOn", timestamp);
                        aggregateNode.setProperty("id", aggregateId);
                    }
                    aggregateNode.setProperty("lastUpdatedOn", timestamp);
                }
            } else if (jsonDomainEvent.isUpdated()) {
                JsonDomainEvent.JsonEntity entity = jsonDomainEvent.getEntity();
                Node node = transaction.findNode(ENTITY_LABEL, "id", entity.getId());

                for (String label : neo4jJsonDomainEvent.labels()) {
                    Label labelUpdate = Label.label(label);
                    if (!node.hasLabel(labelUpdate)) {
                        node.addLabel(labelUpdate);
                    }
                }

                if (node != null) {
                    node.setProperty("lastUpdatedOn", timestamp);

                    attributes(jsonDomainEvent.json(), node);
                    addedRelationships(jsonDomainEvent.json(), transaction, node);
                    removedRelationships(jsonDomainEvent.json(), node);

                    // Update aggregate node
                    if (aggregateType != null && aggregateId != null) {
                        Node aggregateNode = transaction.findNode(AGGREGATE_LABEL, "id", aggregateId);
                        aggregateNode.setProperty("lastUpdatedOn", timestamp);
                    }
                }
            } else if (jsonDomainEvent.isDeleted()) {
                JsonDomainEvent.JsonEntity entity = jsonDomainEvent.getEntity();

                if (entity.getId().equals(aggregateId)) {
                    try (ResourceIterator<Node> entityNodes = transaction.findNodes(ENTITY_LABEL, "aggregateId", aggregateId)) {
                        while (entityNodes.hasNext()) {
                            Node entityNode = entityNodes.next();
                            detachDelete(entityNode);
                        }
                    }

                    Node aggregateNode = transaction.findNode(AGGREGATE_LABEL, "aggregateId", aggregateId);
                    aggregateNode.delete();
                } else {
                    Node node = transaction.findNode(ENTITY_LABEL, "id", entity.getId());
                    detachDelete(node);

                    // Update aggregate node
                    if (aggregateType != null && aggregateId != null) {
                        Node aggregateNode = transaction.findNode(AGGREGATE_LABEL, "id", aggregateId);
                        aggregateNode.setProperty("lastUpdatedOn", timestamp);
                    }
                }
            }
            }
        }
    }

    private void attributes(ObjectNode eventJson, Node node) {
        JsonNode jsonNode;
        jsonNode = eventJson.path("attributes");
        if (jsonNode instanceof ObjectNode attributes) {
            Iterator<String> names = attributes.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                JsonNode value = attributes.get(name);
                if (value instanceof TextNode textNode) {
                    node.setProperty(name, textNode.textValue());
                } else if (value instanceof NumericNode numericNode) {
                    if (numericNode.isFloatingPointNumber()) {
                        node.setProperty(name, numericNode.doubleValue());
                    } else {
                        node.setProperty(name, value.longValue());
                    }
                } else if (value instanceof BooleanNode booleanNode) {
                    node.setProperty(name, booleanNode.booleanValue());
                } else {
                    LogManager.getLogger(getClass()).warn("Could not handle domain event attribute of type:" + value.getNodeType());
                }
            }
        }
    }

    private void addedRelationships(ObjectNode eventJson, Transaction transaction, Node node) {
        JsonNode jsonNode;
        jsonNode = eventJson.path("addedrelationships");
        if (jsonNode instanceof ArrayNode addedRelationships) {
            for (JsonNode addedRelationship : addedRelationships) {
                if (addedRelationship instanceof ObjectNode relationship) {
                    Node relatedNode = transaction.findNode(Label.label(relationship.get("type").textValue()), "id", relationship.get("id").textValue());
                    if (relatedNode != null) {
                        node.createRelationshipTo(relatedNode, RelationshipType.withName(relationship.get("relationship").textValue()));
                    }
                }
            }
        }
    }

    private void removedRelationships(ObjectNode eventJson, Node node) {
        JsonNode jsonNode;
        jsonNode = eventJson.path("removedrelationships");
        if (jsonNode instanceof ArrayNode removedRelationships) {
            for (JsonNode removedRelationship : removedRelationships) {
                if (removedRelationship instanceof ObjectNode relationship) {
                    String id = relationship.get("id").textValue();
                    for (Relationship nodeRelationship : node.getRelationships(RelationshipType.withName(relationship.get("relationship").textValue()))) {
                        Node otherNode = nodeRelationship.getOtherNode(node);
                        if (id.equals(otherNode.getProperty("id"))) {
                            nodeRelationship.delete();
                            break;
                        }
                    }
                }
            }
        }
    }

    private void detachDelete(Node node) {
        for (Relationship relationship : node.getRelationships()) {
            relationship.delete();
        }
        node.delete();
    }
}
