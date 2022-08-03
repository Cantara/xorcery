package com.exoreaction.xorcery.service.opensearch.client.document;

import com.exoreaction.xorcery.service.opensearch.client.ObjectNodeCallback;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchClient;
import com.exoreaction.xorcery.service.opensearch.client.OpenSearchRestProcess;
import com.exoreaction.xorcery.service.opensearch.client.index.CreateComponentTemplateRequest;
import com.exoreaction.xorcery.service.opensearch.client.index.CreateIndexTemplateRequest;
import com.exoreaction.xorcery.service.opensearch.client.index.AcknowledgedResponse;
import com.exoreaction.xorcery.service.opensearch.client.index.ComponentTemplatesResponse;
import com.exoreaction.xorcery.service.opensearch.client.index.IndexTemplatesResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Variant;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record DocumentClient(
        Function<BiConsumer<WebTarget, InvocationCallback<ObjectNode>>, CompletionStage<ObjectNode>> requests) {

    public CompletionStage<BulkResponse> bulk(String indexId, IndexBulkRequest request) {
        return requests.apply((target, callback) -> target.path(indexId).path("_bulk")
                        .request(MediaType.APPLICATION_JSON_TYPE).async()
                        .put(Entity.entity(request, new Variant(MediaType.APPLICATION_JSON_TYPE, "en", "gzip")), callback))
                .thenApply(BulkResponse::new);
    }
}
