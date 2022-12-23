package com.exoreaction.xorcery.service.opensearch.client;

import com.exoreaction.xorcery.process.Process;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public record OpenSearchRestProcess(Supplier<WebTarget> requests,
                                    CompletableFuture<ObjectNode> result,
                                    BiConsumer<WebTarget, InvocationCallback<ObjectNode>> call)
    implements Process<ObjectNode>
{
    @Override
    public void start() {
        call.accept(requests.get(), new ObjectNodeCallback(result));
    }

    @Override
    public void retry() {
        try {
            Thread.sleep(10000);
            start();
        } catch (InterruptedException e) {
            result.cancel(true);
        }
    }
}
