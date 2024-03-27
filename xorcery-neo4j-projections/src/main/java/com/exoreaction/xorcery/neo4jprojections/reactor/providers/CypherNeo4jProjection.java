package com.exoreaction.xorcery.neo4jprojections.reactor.providers;

import com.exoreaction.xorcery.domainevents.api.DomainEvent;
import com.exoreaction.xorcery.domainevents.api.JsonDomainEvent;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4jprojections.reactor.spi.EventsWithTransaction;
import com.exoreaction.xorcery.neo4jprojections.reactor.spi.Neo4jProjection;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CypherNeo4jProjection
        implements Neo4jProjection {
    private final Map<String, List<String>> cachedEventCypher = new HashMap<>();

    @Inject
    public CypherNeo4jProjection() {
    }

    @Override
    public void accept(EventsWithTransaction metadataEventsEventsWithTransaction, SynchronousSink<List<MetadataEvents>> metadataEventsSynchronousSink) {
        List<MetadataEvents> metadataEvents = metadataEventsEventsWithTransaction.event();
        Map<String, Object> metadataMap = null;

        for (MetadataEvents events : metadataEvents) {
            for (DomainEvent event : events.getEvents()) {
                if (event instanceof JsonDomainEvent jsonDomainEvent) {
                    ObjectNode eventJson = jsonDomainEvent.json();

                    String eventName = jsonDomainEvent.getName();
                    List<String> statement = getCypher(eventName);
                    if (statement == null)
                        continue;

                    // Only do this if any of the events are actually projected
                    if (metadataMap == null)
                        metadataMap = Cypher.toMap(events.getMetadata().metadata());

                    Map<String, Object> parameters = Cypher.toMap(eventJson);
                    parameters.put("metadata", metadataMap);

                    for (String stmt : statement) {
                        metadataEventsEventsWithTransaction.transaction().execute(stmt, parameters);
                    }
                }
            }
        }
        metadataEventsSynchronousSink.next(metadataEvents);
    }

    private List<String> getCypher(String type) {
        return cachedEventCypher.computeIfAbsent(type, t ->
        {
            String statementFile = "META-INF/neo4j/event/" + t + ".cyp";
            List<URL> statementFiles = Resources.getResources(statementFile);
            if (statementFiles.isEmpty())
                return null;
            List<String> statements = new ArrayList<>();
            for (URL file : statementFiles) {
                try (InputStream resourceAsStream = file.openStream()) {
                    statements.addAll(List.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8).split(";")));
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            }
            return statements;
        });
    }

}
