package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record IndexClient(
        Function<BiConsumer<WebTarget, InvocationCallback<ObjectNode>>, CompletionStage<ObjectNode>> requests) {

    public CompletionStage<AcknowledgedResponse> createComponentTemplate(String templateId, CreateComponentTemplateRequest request) {
        return requests.apply((target, callback) -> target.path("_component_template").path(templateId)
                        .request(MediaType.APPLICATION_JSON_TYPE).async().put(Entity.json(request.json()), callback))
                .thenApply(AcknowledgedResponse::new);
    }

    public CompletionStage<ComponentTemplatesResponse> getComponentTemplates() {
        return requests.apply((target, callback) -> target.path("_component_template")
                        .request(MediaType.APPLICATION_JSON_TYPE).async().get(callback))
                .thenApply(ComponentTemplatesResponse::new);
    }

    public CompletionStage<AcknowledgedResponse> createIndexTemplate(String templateId, CreateIndexTemplateRequest request) {
        return requests.apply((target, callback) -> target.path("_index_template").path(templateId)
                        .request(MediaType.APPLICATION_JSON_TYPE).async().put(Entity.json(request.json()), callback))
                .thenApply(AcknowledgedResponse::new);
    }

    public CompletionStage<IndexTemplatesResponse> getIndexTemplates() {
        return requests.apply((target, callback) -> target.path("_index_template")
                        .request(MediaType.APPLICATION_JSON_TYPE).async().get(callback))
                .thenApply(IndexTemplatesResponse::new);
    }

    public CompletionStage<Map<String, Index>> getIndices() {
        return requests.apply((target, callback) -> target.path("*")
                        .request(MediaType.APPLICATION_JSON_TYPE).async().get(callback))
                .thenApply(json -> JsonElement.toMap(json, Index::new));
    }

    public CompletionStage<AcknowledgedResponse> deleteIndex(String indexName) {
        return requests.apply((target, callback) -> target.path(indexName)
                        .request(MediaType.APPLICATION_JSON_TYPE).async().delete(callback))
                .thenApply(AcknowledgedResponse::new);
    }
}
