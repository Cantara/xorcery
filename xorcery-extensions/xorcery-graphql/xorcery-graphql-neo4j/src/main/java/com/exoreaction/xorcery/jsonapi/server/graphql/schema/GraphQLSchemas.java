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
package com.exoreaction.xorcery.jsonapi.server.graphql.schema;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.server.graphql.GraphQLExecutor;
import com.exoreaction.xorcery.jsonapi.server.graphql.cypher.CypherWiringFactory;
import com.exoreaction.xorcery.util.Resources;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport;
import graphql.execution.preparsed.persisted.InMemoryPersistedQueryCache;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service(name = "graphql.server")
public class GraphQLSchemas {

    private final GraphQLSchemasConfiguration graphQLSchemasConfiguration;
    private final Map<String, GraphQLSchema> schemas = new ConcurrentHashMap<>();
    private final PreparsedDocumentProvider preparsedDocumentProvider;

    @Inject
    public GraphQLSchemas(Configuration configuration) {
        graphQLSchemasConfiguration = new GraphQLSchemasConfiguration(configuration.getConfiguration("graphql.server.schemas"));

        preparsedDocumentProvider = new ApolloPersistedQuerySupport(new InMemoryPersistedQueryCache(new HashMap<>()));
    }

    public Optional<GraphQLSchema> getSchema(String schemaName) {
        return Optional.ofNullable(schemas.computeIfAbsent(schemaName, n ->
                graphQLSchemasConfiguration.getSchemaConfiguration(n).map(this::createGraphQLSchema).orElse(null)));
    }

    public Optional<GraphQLExecutor> getGraphQLExecutor(String schemaName) {
        return getSchema(schemaName).map(s -> new GraphQLExecutor(s, preparsedDocumentProvider));
    }

    private GraphQLSchema createGraphQLSchema(GraphQLSchemasConfiguration.SchemaConfiguration schemaConfiguration) {

        StringBuilder schemaBuilder = new StringBuilder();
        for (String resource : schemaConfiguration.getResources()) {
            try (InputStream schemaFile = Resources.getResource(resource).orElseThrow().openStream()) {
                String schema = new String(schemaFile.readAllBytes(), StandardCharsets.UTF_8);
                schemaBuilder.append(schema);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        TypeDefinitionRegistry typeDefinitionRegistry = new SchemaParser().parse(schemaBuilder.toString());
        GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
        RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();

        runtimeWiringBuilder.wiringFactory(new CypherWiringFactory());
        GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(
                SchemaGenerator.Options.defaultOptions(),
                typeDefinitionRegistry,
                runtimeWiringBuilder.codeRegistry(codeRegistryBuilder).build()
        );
        return graphQLSchema;
    }
}
