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
package com.exoreaction.xorcery.certificates.client;

import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.jetty.client.CompletableFutureResponseListener;
import com.exoreaction.xorcery.jsonapi.Link;
import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BytesRequestContent;
import org.eclipse.jetty.http.HttpMethod;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class JsonApiClient {

    private ObjectMapper objectMapper = new JsonMapper();

    private HttpClient httpClient;
    private DnsLookup dnsLookup;

    public JsonApiClient(HttpClient httpClient, DnsLookup dnsLookup) {
        this.httpClient = httpClient;
        this.dnsLookup = dnsLookup;
    }

    public CompletionStage<ResourceDocument> get(Link link) {
        return dnsLookup.resolve(link.getHrefAsUri())
                .thenCompose(uris ->
                {
                    CompletableFuture<ContentResponse> future = new CompletableFuture<>();
                    httpClient.newRequest(uris.get(0))
                            .accept(MediaTypes.APPLICATION_JSON_API)
                            .method(HttpMethod.GET)
                            .send(new CompletableFutureResponseListener(future));
                    return future.thenApply(cr ->
                    {
                        try {
                            JsonNode json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
                            return new ResourceDocument((ObjectNode) json);
                        } catch (Throwable e) {
                            throw new CompletionException(e);
                        }
                    });
                }).exceptionallyCompose(t ->
                        CompletableFuture.failedStage(new ProcessingException("Could not connect to "+link.getHref(), t)));
    }

    public CompletionStage<ResourceObject> submit(Link link, ResourceObject resourceObject) {
        return dnsLookup.resolve(link.getHrefAsUri())
                .thenCompose(uris ->
                {
                    CompletableFuture<ContentResponse> future = new CompletableFuture<>();
                    try {
                        byte[] requestBytes = objectMapper.writeValueAsBytes(resourceObject.json());
                        httpClient.newRequest(uris.get(0))
                                .accept(MediaTypes.APPLICATION_JSON_API)
                                .method(HttpMethod.POST)
                                .body(new BytesRequestContent(MediaTypes.APPLICATION_JSON_API, requestBytes))
                                .send(new CompletableFutureResponseListener(future));
                        return future.thenApply(cr ->
                        {
                            try {
                                JsonNode json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
                                return new ResourceObject((ObjectNode) json);
                            } catch (Throwable e) {
                                throw new CompletionException(e);
                            }
                        });
                    } catch (JsonProcessingException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });
    }
}
