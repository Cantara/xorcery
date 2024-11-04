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
package dev.xorcery.opensearch.client;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.jsonapi.providers.JsonNodeMessageBodyReader;
import dev.xorcery.jsonapi.providers.JsonNodeMessageBodyWriter;
import dev.xorcery.opensearch.client.document.DocumentClient;
import dev.xorcery.opensearch.client.index.IndexClient;
import dev.xorcery.opensearch.client.jaxrs.BulkRequestMessageBodyWriter;
import dev.xorcery.opensearch.client.search.SearchClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;

import java.io.Closeable;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

public record OpenSearchClient(Client client, URI host)
    implements Closeable
{

    public OpenSearchClient(ClientBuilder clientBuilder, URI host) {
        this(clientBuilder
                .register(JsonNodeMessageBodyReader.class)
                .register(JsonNodeMessageBodyWriter.class)
                .register(new BulkRequestMessageBodyWriter(new ObjectMapper()
                        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                        .findAndRegisterModules()))
                .build(), host);
    }

    public IndexClient indices() {
        return new IndexClient(this::call);
    }

    public DocumentClient documents() {
        return new DocumentClient(this::call);
    }

    public SearchClient search() {
        return new SearchClient(this::call);
    }

    private WebTarget request() {
        return client.target(host);
    }

    private CompletionStage<ObjectNode> call(BiConsumer<WebTarget, InvocationCallback<ObjectNode>> invocation) {
        OpenSearchRestProcess restProcess = new OpenSearchRestProcess(this::request, new CompletableFuture<>(), invocation);
        restProcess.start();
        return restProcess.result();
    }

    @Override
    public void close() {
        client.close();
    }
}
