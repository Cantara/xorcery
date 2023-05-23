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
package com.exoreaction.xorcery.opensearch.client.document;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

public record DocumentClient(
        Function<BiConsumer<WebTarget, InvocationCallback<ObjectNode>>, CompletionStage<ObjectNode>> requests) {

    public CompletionStage<BulkResponse> bulk(String indexId, IndexBulkRequest request) {
        return requests.apply((target, callback) -> target.path(indexId).path("_bulk")
                        .request(MediaType.APPLICATION_JSON_TYPE).async()
                        .put(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE), callback))
//                        .put(Entity.entity(request, new Variant(MediaType.APPLICATION_JSON_TYPE, "en", "gzip")), callback))
                .thenApply(BulkResponse::new);
    }
}
