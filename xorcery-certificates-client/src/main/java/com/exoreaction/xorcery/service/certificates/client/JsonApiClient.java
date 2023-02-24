package com.exoreaction.xorcery.service.certificates.client;

import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.jetty.client.CompletableFutureResponseListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
                });
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
