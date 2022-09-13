package com.exoreaction.xorcery.service.opensearch.client.search;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record SearchClient(
        Function<BiConsumer<WebTarget, InvocationCallback<ObjectNode>>, CompletionStage<ObjectNode>> requests) {

    public CompletionStage<SearchResponse> search(String indexId, SearchRequest request, Map<String, String> parameters) {
        return requests.apply((target, callback) ->
                {
                    WebTarget webTarget = target.path(indexId).path("_search");
                    for (Map.Entry<String, String> entry : parameters.entrySet()) {
                        webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
                    }
                    webTarget.request(MediaType.APPLICATION_JSON_TYPE).async()
                            .post(Entity.json(request.json()), callback);
                })
                .thenApply(SearchResponse::new);
    }
}
