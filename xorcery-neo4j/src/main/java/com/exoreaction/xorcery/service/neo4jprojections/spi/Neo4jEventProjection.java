package com.exoreaction.xorcery.service.neo4jprojections.spi;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Map;

public interface Neo4jEventProjection {
    boolean isWritable(String eventClass);

    void write(Map<String, Object> metadataMap, ObjectNode eventJson, Transaction transaction) throws IOException;
}
