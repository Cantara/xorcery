package com.exoreaction.xorcery.service.opensearch.eventstore.domainevents;

public interface ProjectionListener {
    void onCommit(String indexId, long revision);
}
