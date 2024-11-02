/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.opensearch.client.index;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.json.JsonElement;
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
