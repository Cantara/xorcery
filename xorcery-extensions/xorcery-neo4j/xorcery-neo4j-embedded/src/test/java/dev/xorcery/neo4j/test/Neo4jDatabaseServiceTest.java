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
package dev.xorcery.neo4j.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.neo4j.Neo4jDatabaseService;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.neo4j.client.GraphResult;
import dev.xorcery.opentelemetry.exporters.local.LocalMetricReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Neo4jDatabaseServiceTest {

    @RegisterExtension
    static XorceryExtension xorceryExtension = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
defaults.development: true
neo4jdatabase.warmup.queries:
    - "MATCH (n) RETURN count(n)"
      
log4j2.Configuration.thresholdFilter.level: debug
log4j2.Configuration.Loggers.logger:
  - name: "dev.xorcery.test"
    level: "debug"
  - name: "dev.xorcery.core"
    level: "debug"                    
                    """)
            .build();

    @Test
    public void startupNeo4j(Neo4jDatabaseService neo4JDatabaseService) throws Exception {
        try (GraphResult graphResult = neo4JDatabaseService.provide().execute("MATCH (n) RETURN count(n)", Collections.emptyMap(), 10).join())
        {
            System.out.println(graphResult.getResult().resultAsString());
        }
    }

    @Test
    public void testMetrics(LocalMetricReader localMetricReader, GraphDatabase graphDatabase) throws Exception {
        for (int i = 0; i < 10000; i++) {
            graphDatabase.execute("CREATE (node:Node) SET node.foo='bar'", Collections.emptyMap(), 90).join().close();
        }

        localMetricReader.getMetric("neo4j.checkpoint.count").orElseThrow().getLongGaugeData().getPoints().forEach(point -> assertTrue(point.getValue() > 0));
        localMetricReader.getMetric("neo4j.pagecache.flushes").orElseThrow().getLongGaugeData().getPoints().forEach(point -> assertTrue(point.getValue() > 0));
    }
}
