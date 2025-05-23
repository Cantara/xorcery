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
package dev.xorcery.neo4jprojections.providers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4j.client.Cypher;
import dev.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import dev.xorcery.util.Resources;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service(name = "cypherprojection")
@ContractsProvided({Neo4jEventProjection.class})
@Rank(10)
public class CypherEventProjection
        implements Neo4jEventProjection {
    private final Logger logger = LogManager.getLogger(getClass());

    private final Map<String, List<String>> cachedEventCypher = new ConcurrentHashMap<>();

    @Override
    public void write(MetadataEvents events, Transaction transaction) {
        Map<String, Object> metadataMap = null;

        for (DomainEvent event : events.data()) {
            if (event instanceof JsonDomainEvent jsonDomainEvent)
            {
                ObjectNode eventJson = jsonDomainEvent.json();

                String eventName = jsonDomainEvent.getName();
                List<String> statement = getCypher(eventName);
                if (statement == null)
                    continue;

                // Only do this if any of the events are actually projected
                if (metadataMap == null)
                    metadataMap = Cypher.toMap(events.metadata().metadata());

                Map<String, Object> parameters = Cypher.toMap(eventJson);

                for (String stmt : statement) {
                    transaction.execute(stmt, Map.of("event", parameters, "metadata", metadataMap));
                }
            }
        }
    }

    private List<String> getCypher(String type) {
        return cachedEventCypher.computeIfAbsent(type, t ->
        {
            String statementFile = "META-INF/neo4j/event/" + t + ".cyp";
            List<URL> statementFiles = Resources.getResources(statementFile);
            if (statementFiles.isEmpty())
                return Collections.emptyList();
            List<String> statements = new ArrayList<>();
            for (URL file : statementFiles) {
                try (InputStream resourceAsStream = file.openStream()) {
                    statements.addAll(List.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8).split(";")));
                } catch (IOException e) {
                    logger.error("Could not load Neo4j event projection Cypher statement:" + statementFile, e);
                    return null;
                }
            }
            return statements;
        });
    }
}
