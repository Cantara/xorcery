package com.exoreaction.xorcery.eventstore;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.StreamMetadata;

import java.util.concurrent.CompletableFuture;

public record ConnectionTestProcess(EventStoreDBClient client, CompletableFuture<StreamMetadata> result)
        implements EventStoreClientProcess<StreamMetadata> {
    @Override
    public void start() {
        client.getStreamMetadata("$all").whenComplete(this::complete);
    }
}
