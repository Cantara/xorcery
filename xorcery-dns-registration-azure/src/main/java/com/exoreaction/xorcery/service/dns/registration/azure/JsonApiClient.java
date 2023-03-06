package com.exoreaction.xorcery.service.dns.registration.azure;

import com.exoreaction.xorcery.service.dns.registration.azure.model.AzureDnsRecordRequest;
import com.exoreaction.xorcery.service.dns.registration.azure.model.AzureTokenResponse;
import com.exoreaction.xorcery.service.jetty.client.CompletableFutureResponseListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.BytesRequestContent;
import org.eclipse.jetty.client.util.FormRequestContent;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class JsonApiClient {
    private static final String APPLICATION_JSON = "application/json";
    private static final String AZURE_OAUTH2_URL_PATTERN = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String AZURE_PRIVATE_DNS_URL_PATTERN = "https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/privateDnsZones/%s/%s/%s";
    private static final String API_VERSION = "2018-09-01";
    private final ObjectMapper objectMapper = new JsonMapper();
    private final HttpClient httpClient;

    public JsonApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public CompletionStage<AzureTokenResponse> token(String azureTenantId, String clientId, String clientSecret) {
        var future = new CompletableFuture<ContentResponse>();
        var fields = new Fields();
        fields.put("client_id", clientId);
        fields.put("client_secret", clientSecret);
        fields.put("scope", "https://management.azure.com/.default");
        fields.put("grant_type", "client_credentials");
        httpClient.newRequest(AZURE_OAUTH2_URL_PATTERN.formatted(azureTenantId))
            .accept(APPLICATION_JSON)
            .method(HttpMethod.POST)
            .body(new FormRequestContent(fields))
            .send(new CompletableFutureResponseListener(future));
        return future.thenApply(cr -> {
           try {
               var json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
               return new AzureTokenResponse((ObjectNode) json);
           } catch (Throwable e) {
               throw new CompletionException(e);
           }
        });
    }

    public CompletionStage<Void> putARecord(
        String azureSubscription, String azureResourceGroup, String azureDnsZone,
        AzureDnsRecordRequest recordObject, String name, String token) {
        var future = new CompletableFuture<ContentResponse>();
        try {
            var requestBytes = objectMapper.writeValueAsBytes(recordObject);
            httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(azureSubscription, azureResourceGroup, azureDnsZone, "A", name))
                .accept(APPLICATION_JSON)
                .param("api-version", API_VERSION)
                .method(HttpMethod.PUT)
                .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
                .body(new BytesRequestContent(APPLICATION_JSON, requestBytes))
                .send(new CompletableFutureResponseListener(future));
            return future.thenApply(cr ->
            {
                try {
                    var json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
                    return null;
                } catch (Throwable e) {
                    throw new CompletionException(e);
                }
            });
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletionStage<Void> deleteARecord(
            String azureSubscription, String azureResourceGroup, String azureDnsZone,
            String name, String token) {
        var future = new CompletableFuture<ContentResponse>();
        httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(azureSubscription, azureResourceGroup, azureDnsZone, "A", name))
            .accept(APPLICATION_JSON)
            .param("api-version", API_VERSION)
            .method(HttpMethod.DELETE)
            .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
            .send(new CompletableFutureResponseListener(future));
        return future.thenApply(cr ->
        {
            try {
                return null;
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletionStage<Void> putTXTRecord(
            String azureSubscription, String azureResourceGroup, String azureDnsZone,
            AzureDnsRecordRequest recordObject, String name, String token) {
        var future = new CompletableFuture<ContentResponse>();
        try {
            var requestBytes = objectMapper.writeValueAsBytes(recordObject);
            httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(azureSubscription, azureResourceGroup, azureDnsZone, "TXT", name))
                .accept(APPLICATION_JSON)
                .param("api-version", API_VERSION)
                .method(HttpMethod.PUT)
                .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
                .body(new BytesRequestContent(APPLICATION_JSON, requestBytes))
                .send(new CompletableFutureResponseListener(future));
            return future.thenApply(cr ->
            {
                try {
                    var json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
                    return null;
                } catch (Throwable e) {
                    throw new CompletionException(e);
                }
            });
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletionStage<Void> deleteTXTRecord(
            String azureSubscription, String azureResourceGroup, String azureDnsZone,
            String name, String token) {
        var future = new CompletableFuture<ContentResponse>();
        httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(azureSubscription, azureResourceGroup, azureDnsZone, "TXT", name))
            .accept(APPLICATION_JSON)
            .param("api-version", API_VERSION)
            .method(HttpMethod.DELETE)
            .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
            .send(new CompletableFutureResponseListener(future));
        return future.thenApply(cr ->
        {
            try {
                return null;
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        });
    }

    public CompletionStage<Void> putSRVRecord(
        String azureSubscription, String azureResourceGroup, String azureDnsZone,
        AzureDnsRecordRequest recordObject, String name, String token) {
        final var future = new CompletableFuture<ContentResponse>();
        try {
            var requestBytes = objectMapper.writeValueAsBytes(recordObject);
            httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(azureSubscription, azureResourceGroup, azureDnsZone, "SRV", name))
                .accept(APPLICATION_JSON)
                .param("api-version", API_VERSION)
                .method(HttpMethod.PUT)
                .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
                .body(new BytesRequestContent(APPLICATION_JSON, requestBytes))
                .send(new CompletableFutureResponseListener(future));
            return future.thenApply(cr ->
            {
                try {
                    var json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
                    return null;
                } catch (Throwable e) {
                    throw new CompletionException(e);
                }
            });
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }


    public CompletionStage<Void> deleteSRVRecord(
            String azureSubscription, String azureResourceGroup, String azureDnsZone,
            String name, String token) {
        var future = new CompletableFuture<ContentResponse>();
        httpClient.newRequest(AZURE_PRIVATE_DNS_URL_PATTERN.formatted(azureSubscription, azureResourceGroup, azureDnsZone, "SRV", name))
            .accept(APPLICATION_JSON)
            .param("api-version", API_VERSION)
            .method(HttpMethod.DELETE)
            .headers(headers -> headers.put(HttpHeader.AUTHORIZATION, "Bearer " + token))
            .send(new CompletableFutureResponseListener(future));
        return future.thenApply(cr ->
        {
            try {
                return null;
            } catch (Throwable e) {
                throw new CompletionException(e);
            }
        });
    }
}
