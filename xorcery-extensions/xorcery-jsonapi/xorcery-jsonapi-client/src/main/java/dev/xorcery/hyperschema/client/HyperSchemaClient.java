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
package dev.xorcery.hyperschema.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.hyperschema.HyperSchema;
import dev.xorcery.jsonapi.Link;
import dev.xorcery.jsonapi.MediaTypes;
import dev.xorcery.jsonschema.JsonSchema;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.InvocationCallback;

import java.util.concurrent.CompletableFuture;

public record HyperSchemaClient(Client client) {

    public CompletableFuture<HyperSchema> get(Link link) {
        CompletableFuture<HyperSchema> future = new CompletableFuture<>();
        client.target(link.getHrefAsUri())
                .request(MediaTypes.APPLICATION_JSON_SCHEMA)
                .buildGet()
                .submit(new InvocationCallback<ObjectNode>() {
                    @Override
                    public void completed(ObjectNode json) {
                        future.complete(new HyperSchema(new JsonSchema(json)));
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                });
        return future;
    }
}
