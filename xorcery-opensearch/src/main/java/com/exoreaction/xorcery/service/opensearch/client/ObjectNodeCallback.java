package com.exoreaction.xorcery.service.opensearch.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.InvocationCallback;

import java.util.concurrent.CompletableFuture;

public record ObjectNodeCallback(CompletableFuture<ObjectNode> future)
    implements InvocationCallback<ObjectNode>
{
    @Override
    public void completed(ObjectNode objectNode) {
        future.complete(objectNode);
    }

    @Override
    public void failed(Throwable throwable) {
        future.completeExceptionally(throwable);
    }
}
