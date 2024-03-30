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
package com.exoreaction.xorcery.jsonapi.server.graphql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.schema.GraphQLSchema;

import java.util.concurrent.CompletableFuture;

public class GraphQLExecutor {

    private final GraphQLSchema schema;
    private final PreparsedDocumentProvider preparsedDocumentProvider;

    public GraphQLExecutor(GraphQLSchema schema, PreparsedDocumentProvider preparsedDocumentProvider) {
        this.schema = schema;
        this.preparsedDocumentProvider = preparsedDocumentProvider;
    }

    public CompletableFuture<ExecutionResult> executeQuery(ExecutionInput executionInput) {
        GraphQL graphQL = GraphQL.newGraphQL(schema)
                .preparsedDocumentProvider(preparsedDocumentProvider)
                .build();

        return graphQL.executeAsync(executionInput);
    }
}
