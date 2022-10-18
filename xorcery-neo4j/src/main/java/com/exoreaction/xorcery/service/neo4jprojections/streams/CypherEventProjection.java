package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CypherEventProjection
        implements Neo4jEventProjection {
    private final Logger logger = LogManager.getLogger(getClass());

    private final Map<String, List<String>> cachedEventCypher = new HashMap<>();

    @Override
    public boolean isWritable(String eventClass) {
        return getCypher(eventClass) != null;
    }

    @Override
    public void write(Map<String, Object> metadataMap, ObjectNode eventJson, Transaction transaction)
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
