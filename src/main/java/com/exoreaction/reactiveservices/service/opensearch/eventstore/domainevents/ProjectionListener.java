package com.exoreaction.reactiveservices.service.opensearch.eventstore.domainevents;

public interface ProjectionListener {
    void onCommit(String streamId, long revision);
}
