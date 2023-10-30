package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjectionPreProcessor;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.EventHandler;

public class PreProcessorEventHandler
    implements EventHandler<WithMetadata<ArrayNode>>
{
    private final Neo4jEventProjectionPreProcessor preProcessor;

    public PreProcessorEventHandler(Neo4jEventProjectionPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    public void onEvent(WithMetadata<ArrayNode> event, long sequence, boolean endOfBatch) throws Exception {
        preProcessor.preProcess(event);
    }
}
