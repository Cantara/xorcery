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
package com.exoreaction.xorcery.graphql.neo4j.cypher;

import graphql.execution.DataFetcherResult;
import graphql.language.Document;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CypherDataFetcher implements DataFetcher<Object> {

    private final Map<Document, List<String>> queryCypher = new HashMap<>();

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {

        List<String> cypher = queryCypher.computeIfAbsent(environment.getDocument(), this::createCypher);

        // TODO Run Cypher query here
        Map<String, Object> keanu = Map.of("__typename","Actor","id", "a1", "name", "Keanu Reeves");
        Map<String, Object> cam = Map.of("__typename","Actor","id", "a2", "name", "Carrie-Anne Moss");

        List<Map<String, Object>> data = List.of(
                Map.of("id", "m1", "title", "The Matrix", "Actors", List.of(keanu, cam)),
                Map.of("id", "m2", "title", "Matrix Reloaded", "Actors", List.of(keanu, cam))
        );

        return DataFetcherResult.newResult().data(data).build();
    }

    private List<String> createCypher(Document document) {
        return List.of("MATCH (n) RETURN n");
    }
}
