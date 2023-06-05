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
package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service(name="cypherprojection")
@ContractsProvided({Neo4jEventProjection.class})
public class CypherEventProjection
        implements Neo4jEventProjection {
    private final Logger logger = LogManager.getLogger(getClass());

    private final Map<String, List<String>> cachedEventCypher = new HashMap<>();

    @Override
    public boolean isWritable(String eventClass) {
        return getCypher(eventClass) != null;
    }

    @Override
    public void write(WithMetadata<ArrayNode> events, Map<String, Object> metadataMap, ObjectNode eventJson, int eventIndex, Transaction transaction)
            throws IOException {

        String type = eventJson.path("@class").textValue();
        List<String> statement = getCypher(type);
        if (statement == null)
            return;

        Map<String, Object> parameters = Cypher.toMap(eventJson);
        parameters.put("metadata", metadataMap);

        for (String stmt : statement) {
            try {
                transaction.execute(stmt, parameters);
            } catch (Throwable e) {
                logger.error(String.format("Could not apply Neo4j statement for event %s (metadata:%s,parameters:%s):\n%s", type, metadataMap.toString(), parameters.toString(), stmt), e);
                throw e;
            }
        }
    }

    private List<String> getCypher(String type) {
        return cachedEventCypher.computeIfAbsent(type, t ->
        {
            String statementFile = "META-INF/neo4j/event/" + t + ".cyp";
            try (InputStream resourceAsStream = ClassLoader.getSystemResourceAsStream(statementFile)) {
                if (resourceAsStream == null)
                    return null;

                return List.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8).split(";"));
            } catch (IOException e) {
                logger.error("Could not load Neo4j event projection Cypher statement:" + statementFile, e);
                return null;
            }
        });

    }
}
