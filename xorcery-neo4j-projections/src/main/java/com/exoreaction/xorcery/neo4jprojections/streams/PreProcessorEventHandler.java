package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjectionPreProcessor;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;

public class PreProcessorEventHandler
    implements EventHandler<WithMetadata<ArrayNode>>
{
    private final Neo4jEventProjectionPreProcessor preProcessor;
    private Sequence sequenceCallback;

    public PreProcessorEventHandler(Neo4jEventProjectionPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {
        this.sequenceCallback = sequenceCallback;
    }

    @Override
    public void onEvent(WithMetadata<ArrayNode> event, long sequence, boolean endOfBatch) throws Exception {

        try {
            preProcessor.preProcess(event);
        } catch (EarlyReleaseException e) {
            // Release processed events
            sequenceCallback.set(sequence);
            // Try again
            preProcessor.preProcess(event);
        }
    }
}
