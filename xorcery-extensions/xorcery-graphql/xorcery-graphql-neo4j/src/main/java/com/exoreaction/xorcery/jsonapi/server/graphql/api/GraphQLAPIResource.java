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
package com.exoreaction.xorcery.jsonapi.server.graphql.api;

import com.exoreaction.xorcery.jaxrs.server.resources.BaseResource;
import com.exoreaction.xorcery.jsonapi.Error;
import com.exoreaction.xorcery.jsonapi.*;
import com.exoreaction.xorcery.jsonapi.server.graphql.schema.GraphQLSchemas;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.jsonapischema.ResourceDocumentSchema;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.server.resources.JsonSchemaResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.exoreaction.xorcery.jsonapi.JsonApiRels.describedby;
import static com.exoreaction.xorcery.jsonapi.JsonApiRels.self;

@Path("api/graphql")
public class GraphQLAPIResource
        extends BaseResource
        implements JsonApiResource, JsonSchemaResource {

    private static final JsonMapper jsonMapper = new JsonMapper();

    private final GraphQLSchemas graphQLSchemas;

    @Inject
    public GraphQLAPIResource(GraphQLSchemas graphQLSchemas) {
        this.graphQLSchemas = graphQLSchemas;
    }

    @GET
    @Path("{schema}")
    public Response get(
            @PathParam("schema") String schemaName,
            @QueryParam("graphql[query]") String query,
            @QueryParam("graphql[variables]") String variables) throws JsonProcessingException {

        if (query == null) {
            String statusTemplate = getAbsolutePathBuilder()
                    .queryParam("graphql[query]", "{query}")
                    .queryParam("graphql[variables]", "{variables}")
                    .toTemplate();
            return Response.ok(new ResourceDocument.Builder()
                    .links(new Links.Builder()
                            .link(self, statusTemplate)
                            .link(describedby, getBaseUriBuilder().path(GraphQLAPIResource.class).path(".schema").toTemplate())
                            .build())
                    .build()).build();
        }

        long start = System.currentTimeMillis();
         ExecutionInput.Builder executionInputBuilder = ExecutionInput.newExecutionInput(query);
        if (variables != null && !variables.isBlank()) {
            Map<String, Object> variableMap = jsonMapper.readValue(variables, Map.class);
            executionInputBuilder.variables(variableMap);
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            BigInteger bigInteger = new BigInteger(1, messageDigest.digest(query.getBytes(StandardCharsets.UTF_8)));
            String calculatedChecksum = String.format("%064x", bigInteger);
            executionInputBuilder.extensions(Map.of("persistedQuery", Map.of("sha256Hash", calculatedChecksum)));
        } catch (NoSuchAlgorithmException e) {
            // Ignore
        }



        ExecutionInput executionInput = executionInputBuilder.build();

        ResourceDocument resourceDocument = graphQLSchemas.getGraphQLExecutor(schemaName).map(e ->
        {
            ExecutionResult result = e.executeQuery(executionInput).join();

            if (!result.getErrors().isEmpty()) {
                return new ResourceDocument.Builder()
                        .errors(new Errors.Builder().with(err -> result.getErrors().forEach(error -> err.error(new Error.Builder().title(error.getMessage()).build()))))
                        .build();
            }
            Object data = result.getData();
            if (data instanceof Map<?, ?> dataMap) {
                ResourceDocument.Builder resourceDocumentBuilder = new ResourceDocument.Builder();
                ResourceObjects.Builder resourceObjectsBuilder = new ResourceObjects.Builder();
                Included.Builder included = new Included.Builder();
                for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                    String type = entry.getKey().toString();
                    if (entry.getValue() instanceof List resourceList) {
                        for (Object resource : resourceList) {
                            if (resource instanceof Map<?, ?> resourceMap) {
                                String id = (String)resourceMap.get("id");
                                ResourceObject.Builder resourceBuilder = new ResourceObject.Builder(type, id);
                                Attributes.Builder attributesBuilder = null;
                                Relationships.Builder relationshipsBuilder = null;
                                for (Map.Entry<?, ?> resourceEntry : resourceMap.entrySet()) {
                                    if (resourceEntry.getValue() instanceof Map<?, ?> relatedResource) {
                                        // TODO
                                    } else if (resourceEntry.getValue() instanceof List<?> relatedResources) {
                                        if (relationshipsBuilder == null) {
                                            relationshipsBuilder = new Relationships.Builder();
                                        }
                                        Relationship.Builder relationshipBuilder = new Relationship.Builder();
                                        ResourceObjectIdentifiers.Builder resourceIdentifiers = new ResourceObjectIdentifiers.Builder();

                                        for (Object relatedResource : relatedResources) {
                                            ResourceObjectIdentifier identifier = includeResource((Map<?, ?>) relatedResource, included);
                                            resourceIdentifiers.resource(identifier);
                                        }
                                        relationshipsBuilder.relationship(resourceEntry.getKey().toString(), relationshipBuilder
                                                .resourceIdentifiers(resourceIdentifiers.build())
                                                .build());
                                        // TODO
                                    } else {
                                        if (attributesBuilder == null) {
                                            attributesBuilder = new Attributes.Builder();
                                        }
                                        attributesBuilder.attribute(resourceEntry.getKey().toString(), resourceEntry.getValue());
                                    }
                                }
                                if (attributesBuilder != null) {
                                    resourceBuilder.attributes(attributesBuilder);
                                }
                                if (relationshipsBuilder != null) {
                                    resourceBuilder.relationships(relationshipsBuilder.build());
                                }
                                resourceObjectsBuilder.resource(resourceBuilder.build());
                            }
                        }
                    }
                }
                resourceDocumentBuilder.data(resourceObjectsBuilder.build());
                resourceDocumentBuilder.included(included.build());
                return resourceDocumentBuilder.build();
            }
            throw new IllegalStateException();
        }).orElse(null);

        long end = System.currentTimeMillis();
        LogManager.getLogger().info("Query time:"+(end-start));
        return Response.ok(resourceDocument)
                .header("Server-Timing", "query;dur="+(end-start))
                .build();
    }

    private ResourceObjectIdentifier includeResource(Map<?, ?> relatedMap, Included.Builder included) {

        String type = (String) relatedMap.get("__typename");
        String id = (String) relatedMap.get("id");
        if (id != null && type != null) {
            if (included.hasIncluded(id, type))
                return new ResourceObjectIdentifier.Builder(type, id).build();
        }

        if (id == null)
            id = UUID.randomUUID().toString();
        if (type == null)
            type = "unknown";
        ResourceObjectIdentifier roi = new ResourceObjectIdentifier.Builder(type, id).build();

        ResourceObject.Builder resourceBuilder = new ResourceObject.Builder(type, id);
        Attributes.Builder attributesBuilder = null;
        Relationships.Builder relationshipsBuilder = null;
        for (Map.Entry<?, ?> resourceEntry : relatedMap.entrySet()) {
            if (resourceEntry.getValue() instanceof Map<?, ?> relatedResource) {
                // TODO
            } else if (resourceEntry.getValue() instanceof List<?> relatedResources) {
                if (relationshipsBuilder == null) {
                    relationshipsBuilder = new Relationships.Builder();
                }
                Relationship.Builder relationshipBuilder = new Relationship.Builder();
                ResourceObjectIdentifiers.Builder resourceIdentifiers = new ResourceObjectIdentifiers.Builder();

                for (Object relatedResource : relatedResources) {
                    ResourceObjectIdentifier identifier = includeResource((Map<?, ?>) relatedResource, included);
                    resourceIdentifiers.resource(identifier);
                }
                relationshipsBuilder.relationship(resourceEntry.getKey().toString(), relationshipBuilder.build());
                // TODO
            } else {
                if (attributesBuilder == null) {
                    attributesBuilder = new Attributes.Builder();
                }
                attributesBuilder.attribute(resourceEntry.getKey().toString(), resourceEntry.getValue());
            }
        }
        if (attributesBuilder != null) {
            resourceBuilder.attributes(attributesBuilder);
        }
        included.include(resourceBuilder.build());
        return roi;
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        return new ResourceDocumentSchema.Builder()
                .builder()
                .links(new com.exoreaction.xorcery.hyperschema.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .with(l -> l.link(new com.exoreaction.xorcery.hyperschema.Link.UriTemplateBuilder("self")
                                .parameter("graphql[query]", "GraphQL query", "GraphQL of data to include in response")
                                .parameter("graphql[variables]", "GraphQL query variables", "Variables for the query")
                                .build()))
                        .build())
                .builder()
                .title("Status")
                .build();
    }

}
