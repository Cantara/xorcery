package com.exoreaction.xorcery.service.neo4j.resources.api;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.jsonapi.model.*;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("api/neo4j")
public class Neo4jResource
        extends JsonApiResource {
    private GraphDatabase graphDatabase;

    @Inject
    public Neo4jResource(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    @GET
    public CompletionStage<ResourceDocument> get(@QueryParam("query") String query, @QueryParam("parameters") String parameters) throws JsonProcessingException {
        if (query == null) {
            return CompletableFuture.completedStage(new ResourceDocument.Builder().build());
        } else {
            Map<String, Object> parameterMap = Collections.emptyMap();
            if (parameters != null && !parameters.trim().equals("")) {
                ObjectMapper objectMapper = new ObjectMapper();
                parameterMap = Cypher.toMap((ObjectNode) objectMapper.readTree(parameters));
            }

            return graphDatabase.execute(query, parameterMap, 0).thenCompose(result ->
            {
                try (result) {

                    Result resultResult = result.getResult();

                    if (resultResult.getQueryExecutionType().isExplained()) {
                        ExecutionPlanDescription description = resultResult.getExecutionPlanDescription();

                        ObjectNode descriptionJson = toJson(description);

                        String descriptionYaml = new ObjectMapper(new YAMLFactory()).writeValueAsString(descriptionJson);

                        return CompletableFuture.completedStage(new ResourceDocument.Builder()
                                .meta(new Meta.Builder()
                                        .meta("parameters", JsonNodeFactory.instance.textNode(parameters))
                                        .meta("query", JsonNodeFactory.instance.textNode(query))
                                        .build())
                                .data(new ResourceObjects.Builder().resource(new ResourceObject.Builder("executionplan")
                                        .attributes(new Attributes.Builder().attribute("plan", descriptionYaml)).build()))
                                .build());
                    } else {
                        ResourceObjects.Builder builder = new ResourceObjects.Builder();
                        while (resultResult.hasNext()) {
                            Map<String, Object> row = resultResult.next();

                            // Check if we should use a node projection
                            if (row.size() == 1 && row.values().iterator().next() instanceof Node node) {
                                ResourceObject.Builder resourceBuilder = new ResourceObject.Builder(getType(node), getId(node));
                                resourceBuilder.attributes(new Attributes((ObjectNode) Cypher.toJsonNode(node)));

                                Relationships.Builder relationships = new Relationships.Builder();
                                Map<String, ResourceObjectIdentifiers.Builder> rels = new HashMap<>();
                                for (Relationship relationship : node.getRelationships(Direction.OUTGOING)) {
                                    ResourceObjectIdentifiers.Builder roiBuilder = rels.computeIfAbsent(relationship.getType().name(), name -> new ResourceObjectIdentifiers.Builder());
                                    Node otherNode = relationship.getOtherNode(node);
                                    roiBuilder.resource(new ResourceObjectIdentifier.Builder(getType(otherNode), getId(otherNode)).build());
                                }
                                for (Map.Entry<String, ResourceObjectIdentifiers.Builder> entry : rels.entrySet()) {
                                    relationships.relationship(entry.getKey(), new com.exoreaction.xorcery.jsonapi.model.Relationship.Builder().resourceIdentifiers(entry.getValue().build()));
                                }
                                resourceBuilder.relationships(relationships);
                                builder.resource(resourceBuilder.build());

                            } else {
                                ResourceObject.Builder resourceBuilder = new ResourceObject.Builder("row");
                                resourceBuilder.attributes(new Attributes(Cypher.toObjectNode(row)));
                                builder.resource(resourceBuilder.build());
                            }
                        }
                        return CompletableFuture.completedStage(new ResourceDocument.Builder()
                                .meta(new Meta.Builder()
                                        .meta("parameters", JsonNodeFactory.instance.textNode(parameters))
                                        .meta("query", JsonNodeFactory.instance.textNode(query))
                                        .build())
                                .data(builder).build());
                    }

                } catch (Exception ex) {
                    return CompletableFuture.failedStage(ex);
                }
            });
        }
    }

    private String getType(Node node) {
        String type = "Entity";
        for (Label label : node.getLabels()) {
            if (!type.equals(label.name()))
                type = label.name();
        }
        return type;
    }

    private String getId(Node node) {
        try {
            return (String) node.getProperty("id");
        } catch (NotFoundException e) {
            return "";
        }
    }

    private ObjectNode toJson(ExecutionPlanDescription description) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("name", node.textNode(description.getName()));
        node.set("identifiers", description.getIdentifiers().stream().map(node::textNode).collect(JsonElement.toArray()));

        ObjectNode arguments = node.objectNode();
        description.getArguments().forEach((k, v) -> arguments.set(k, node.textNode(v.toString())));
        node.set("arguments", arguments);

        if (description.hasProfilerStatistics()) {
            ObjectNode statistics = node.objectNode();
            ExecutionPlanDescription.ProfilerStatistics profilerStatistics = description.getProfilerStatistics();
            if (profilerStatistics.hasDbHits())
                statistics.set("dbhits", node.numberNode(profilerStatistics.getDbHits()));
            if (profilerStatistics.hasPageCacheStats()) {
                statistics.set("pagecachehits", node.numberNode(profilerStatistics.getPageCacheHits()));
                statistics.set("pagecachemisses", node.numberNode(profilerStatistics.getPageCacheMisses()));
                statistics.set("pagecachehitratio", node.numberNode(profilerStatistics.getPageCacheHitRatio()));
            }
            if (profilerStatistics.hasRows())
                statistics.set("rows", node.numberNode(profilerStatistics.getRows()));
            if (profilerStatistics.hasTime())
                statistics.set("time", node.numberNode(profilerStatistics.getTime()));
        }

        ArrayNode children = node.arrayNode();
        for (ExecutionPlanDescription child : description.getChildren()) {
            children.add(toJson(child));
        }
        node.set("children", children);
        return node;
    }
}
