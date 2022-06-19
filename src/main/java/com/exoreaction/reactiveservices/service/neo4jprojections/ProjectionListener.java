package com.exoreaction.reactiveservices.service.neo4jprojections;

public interface ProjectionListener {
    void onCommit(String streamId, long revision);
}
