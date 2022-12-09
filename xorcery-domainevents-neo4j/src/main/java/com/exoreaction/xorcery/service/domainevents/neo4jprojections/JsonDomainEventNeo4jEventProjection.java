package com.exoreaction.xorcery.service.domainevents.neo4jprojections;

import com.exoreaction.xorcery.service.domainevents.api.event.JsonDomainEvent;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.logging.log4j.LogManager;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Service(name = "jsondomaineventprojection")
@ContractsProvided({Neo4jEventProjection.class})
public class JsonDomainEventNeo4jEventProjection
        implements Neo4jEventProjection {

    public static final Label ENTITY_LABEL = Label.label("Entity");
    public static final Label AGGREGATE_LABEL = Label.label("Aggregate");

    @Override
    public boolean isWritable(String eventClass) {
        return eventClass.equals(JsonDomainEvent.class.getName());
    }

    @Override
    public void write(Map<String, Object> metadataMap, ObjectNode eventJson, Transaction transaction) throws IOException {

        Neo4jJsonDomainEvent neo4jJsonDomainEvent = new Neo4jJsonDomainEvent(new JsonDomainEvent(eventJson));
        Object timestamp = metadataMap.get("timestamp");
        Object aggregateId = metadataMap.get("aggregateId");
        String aggregateType = metadataMap.get("aggregateType").toString();
        if (eventJson.has("created")) {
            ObjectNode created = (ObjectNode) eventJson.path("created");
            Node node = transaction.createNode(Label.label(created.get("type").textValue()), ENTITY_LABEL);

            for (String label : neo4jJsonDomainEvent.labels()) {
                node.addLabel(Label.label(label));
            }

            node.setProperty("created_on", timestamp);
            node.setProperty("last_updated_on", timestamp);
            String id = created.get("id").textValue();
            node.setProperty("id", id);
            if (aggregateId != null) {
                node.setProperty("aggregate_id", aggregateId);
            }

            attributes(eventJson, node);
            addedRelationships(eventJson, transaction, node);
            removedRelationships(eventJson, node);

            // Create/update aggregate node
            if (aggregateType != null && aggregateId != null) {
                Node aggregateNode = null;
                if (!id.equals(aggregateId)) {
                    aggregateNode = transaction.findNode(AGGREGATE_LABEL, "id", aggregateId);
                }

                if (aggregateNode == null) {
                    aggregateNode = transaction.createNode(Label.label(aggregateType), AGGREGATE_LABEL);
                    aggregateNode.setProperty("created_on", timestamp);
                    aggregateNode.setProperty("id", aggregateId);
                }
                aggregateNode.setProperty("last_updated_on", timestamp);
            }
        } else if (eventJson.has("updated")) {
            ObjectNode updated = (ObjectNode) eventJson.path("updated");
            Node node = transaction.findNode(ENTITY_LABEL, "id", updated.get("id").textValue());

            for (String label : neo4jJsonDomainEvent.labels()) {
                Label labelUpdate = Label.label(label);
                if (!node.hasLabel(labelUpdate)) {
                    node.addLabel(labelUpdate);
                }
            }

            if (node != null) {
                node.setProperty("last_updated_on", timestamp);

                attributes(eventJson, node);
                addedRelationships(eventJson, transaction, node);
                removedRelationships(eventJson, node);

                // Update aggregate node
                if (aggregateType != null && aggregateId != null) {
                    Node aggregateNode = transaction.findNode(AGGREGATE_LABEL, "id", aggregateId);
                    aggregateNode.setProperty("last_updated_on", timestamp);
                }
            }
        } else if (eventJson.has("deleted")) {
            ObjectNode deleted = (ObjectNode) eventJson.path("deleted");
            String id = deleted.get("id").textValue();

            if (id.equals(aggregateId)) {
                try (ResourceIterator<Node> entityNodes = transaction.findNodes(ENTITY_LABEL, "aggregate_id", aggregateId)) {
                    while (entityNodes.hasNext()) {
                        Node entityNode = entityNodes.next();
                        detachDelete(entityNode);
                    }
                }

                Node aggregateNode = transaction.findNode(AGGREGATE_LABEL, "aggregate_id", aggregateId);
                aggregateNode.delete();
            } else {
                Node node = transaction.findNode(ENTITY_LABEL, "id", id);
                detachDelete(node);

                // Update aggregate node
                if (aggregateType != null && aggregateId != null) {
                    Node aggregateNode = transaction.findNode(AGGREGATE_LABEL, "id", aggregateId);
                    aggregateNode.setProperty("last_updated_on", timestamp);
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
                        if (id == null || id.equals(otherNode.getProperty("id"))) {
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
