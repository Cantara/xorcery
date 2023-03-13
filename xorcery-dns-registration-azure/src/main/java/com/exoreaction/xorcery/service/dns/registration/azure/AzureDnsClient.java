package com.exoreaction.xorcery.service.dns.registration.azure;

import com.exoreaction.xorcery.service.dns.registration.azure.model.AzureDnsRecord;
import com.exoreaction.xorcery.service.dns.registration.azure.model.AzureDnsResponse;
import com.exoreaction.xorcery.service.dns.registration.azure.model.RecordType;
import com.exoreaction.xorcery.service.jetty.client.CompletableFutureResponseListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BytesRequestContent;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;

public record AzureDnsClient(
        HttpClient httpClient,
        String token,
        String subscription,
        String resourceGroup,
        String dnsZone
    ) {
    private static final String AZURE_PRIVATE_DNS_URL_PATTERN = "https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/privateDnsZones/%s/%s";
    private static final String API_VERSION = "2018-09-01";

    private static final ObjectMapper objectMapper = new JsonMapper();

    public CompletionStage<AzureDnsResponse> listRecords(String nextLink) {
        var future = new CompletableFuture<ContentResponse>();
        httpClient.newRequest("".equals(nextLink) ? AZURE_PRIVATE_DNS_URL_PATTERN.formatted(subscription, resourceGroup, dnsZone, "ALL") : nextLink)
            .accept(APPLICATION_JSON.asString())
            .param("api-version", API_VERSION)
            .method(HttpMethod.GET)
            .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
            .send(new CompletableFutureResponseListener(future));
        return future.thenApply(this::handleResponse);
    }

    public CompletionStage<AzureDnsResponse> putRecord(AzureDnsRecord recordObject, String name, RecordType recordType) {
        var future = new CompletableFuture<ContentResponse>();
        try {
            var requestBytes = objectMapper.writeValueAsBytes(recordObject);
            httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(subscription, resourceGroup, dnsZone, recordType.name()) + "/" + name)
                .accept(APPLICATION_JSON.asString())
                .param("api-version", API_VERSION)
                .method(HttpMethod.PUT)
                .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
                .body(new BytesRequestContent(APPLICATION_JSON.asString(), requestBytes))
                .send(new CompletableFutureResponseListener(future));
            return future.thenApply(this::handleResponse);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletionStage<AzureDnsResponse> patchRecord(AzureDnsRecord recordObject, String name, RecordType recordType) {
        var future = new CompletableFuture<ContentResponse>();
        try {
            var requestBytes = objectMapper.writeValueAsBytes(recordObject);
            httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(subscription, resourceGroup, dnsZone, recordType.name()) + "/" + name)
                .accept(APPLICATION_JSON.asString())
                .param("api-version", API_VERSION)
                .method(HttpMethod.PATCH)
                .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
                .body(new BytesRequestContent(APPLICATION_JSON.asString(), requestBytes))
                .send(new CompletableFutureResponseListener(future));
            return future.thenApply(this::handleResponse);
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletionStage<Void> deleteRecord(String name, RecordType recordType) {
        var future = new CompletableFuture<ContentResponse>();
        httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(subscription, resourceGroup, dnsZone, recordType.name()) + "/" + name)
            .accept(APPLICATION_JSON.asString())
            .param("api-version", API_VERSION)
            .method(HttpMethod.DELETE)
            .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
            .send(new CompletableFutureResponseListener(future));
        return future.thenApply(this::handleResponse)
                .thenApply(cr -> null);
    }

    private AzureDnsResponse handleResponse(ContentResponse cr) {
        switch (cr.getStatus()) {
            case 200:
            case 201:
                break;
            case 204:
                return null;
            default: throw new WebApplicationException(cr.getReason(), cr.getStatus());
        }

        try {
            var json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
            return new AzureDnsResponse((ObjectNode) json);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
