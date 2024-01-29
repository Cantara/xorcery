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
package com.exoreaction.xorcery.neo4j.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.neo4j.Neo4jService;
import com.exoreaction.xorcery.neo4j.client.GraphResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Neo4jServiceTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .build();

    @Test
    public void startupNeo4j() throws Exception {
        Neo4jService neo4jService = xorceryExtension.getXorcery().getServiceLocator().getService(Neo4jService.class);
        try (GraphResult graphResult = neo4jService.provide().execute("MATCH (n) RETURN count(n)", Collections.emptyMap(), 10).toCompletableFuture().join())
        {
            System.out.println(graphResult.getResult().resultAsString());
        }
    }
}
