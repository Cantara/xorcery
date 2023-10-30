package com.exoreaction.xorcery.neo4jprojections.spi;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;

public interface Neo4jEventProjectionPreProcessor {
    void preProcess(WithMetadata<ArrayNode> events) throws Exception;
}
