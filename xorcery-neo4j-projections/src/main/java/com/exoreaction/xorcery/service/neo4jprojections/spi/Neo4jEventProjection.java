package com.exoreaction.xorcery.service.neo4jprojections.spi;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Map;

public interface Neo4jEventProjection {
    boolean isWritable(String eventClass);

    void write(WithMetadata<ArrayNode> events, Map<String, Object> metadataMap, ObjectNode eventJson, Transaction transaction) throws IOException;
}
