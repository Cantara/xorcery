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
package com.exoreaction.xorcery.jsonapi.server.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.graphql.schema.GraphQLSchemas;
import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import com.exoreaction.xorcery.jsonapi.ResourceObjects;
import com.exoreaction.xorcery.junit.XorceryExtension;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

public class GraphQLExecutorTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void testQuery()
    {
        GraphQLSchemas schemas = xorceryExtension.getServiceLocator().getService(GraphQLSchemas.class);
        System.out.println(schemas.getSchema("testschema").orElseThrow());

        ResourceDocument resourceDocument = schemas.getGraphQLExecutor("testschema").map(e ->
        {
            ExecutionResult result = e.executeQuery(ExecutionInput.newExecutionInput("""
query {
    Movie {
        id
        __typename
        title
        Actors {
        id
        __typename
        name
        }
    }
}
                    """).build()).join();

            for (GraphQLError error : result.getErrors()) {
                System.out.println(error);
            }
            Object data = result.getData();
            if (data instanceof Map<?,?> dataMap)
            {
                ResourceDocument.Builder resourceDocumentBuilder = new ResourceDocument.Builder();
                ResourceObjects.Builder resourceObjectsBuilder = new ResourceObjects.Builder();
                for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                    String type = entry.getKey().toString();
                    if (entry.getValue() instanceof List resourceList)
                    {
                        for (Object resource : resourceList) {
                            if (resource instanceof Map<?,?> resourceMap)
                            {
                                ResourceObject.Builder resourceBuilder = new ResourceObject.Builder(type);
                                Attributes.Builder attributesBuilder = null;
                                for (Map.Entry<?, ?> resourceEntry : resourceMap.entrySet()) {
                                    if (resourceEntry.getValue() instanceof Map<?,?> relatedSource)
                                    {
                                        // TODO
                                    } else
                                    {
                                        if (attributesBuilder == null)
                                        {
                                            attributesBuilder = new Attributes.Builder();
                                        }
                                        attributesBuilder.attribute(resourceEntry.getKey().toString(), resourceEntry.getValue());
                                    }
                                }
                                if (attributesBuilder != null)
                                {
                                    resourceBuilder.attributes(attributesBuilder);
                                }
                                resourceObjectsBuilder.resource(resourceBuilder.build());
                            }
                        }
                    }
                }
                resourceDocumentBuilder.data(resourceObjectsBuilder.build());
                return resourceDocumentBuilder.build();
            }
            throw new IllegalStateException();
        }).orElse(null);

        System.out.println(resourceDocument.json().toPrettyString());
    }
}
