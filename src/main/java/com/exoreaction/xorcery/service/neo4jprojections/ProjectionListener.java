package com.exoreaction.xorcery.service.neo4jprojections;

public interface ProjectionListener {
    void onCommit(String streamId, long revision);
}
