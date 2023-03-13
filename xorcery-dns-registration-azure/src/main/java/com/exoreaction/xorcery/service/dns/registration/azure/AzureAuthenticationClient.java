package com.exoreaction.xorcery.service.dns.registration.azure;

import com.exoreaction.xorcery.service.dns.registration.azure.model.AzureConfiguration;
import com.exoreaction.xorcery.service.dns.registration.azure.model.AzureTokenResponse;
import com.exoreaction.xorcery.service.jetty.client.CompletableFutureResponseListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.FormRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.Fields;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class AzureAuthenticationClient {
    private static final String APPLICATION_JSON = "application/json";
    private static final String AZURE_OAUTH2_URL_PATTERN = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private final ObjectMapper objectMapper = new JsonMapper();
    private final HttpClient httpClient;
    private final AzureConfiguration configuration;


    public AzureAuthenticationClient(HttpClient httpClient, AzureConfiguration configuration) {
        this.httpClient = httpClient;
        this.configuration = configuration;
    }

    public CompletionStage<AzureDnsClient> loginDns() {
        var future = new CompletableFuture<ContentResponse>();
        var fields = new Fields();
        fields.put("client_id", configuration.getClientId());
        fields.put("client_secret", configuration.getClientSecret());
        fields.put("scope", "https://management.azure.com/.default");
        fields.put("grant_type", "client_credentials");
        httpClient.newRequest(AZURE_OAUTH2_URL_PATTERN.formatted(configuration.getTenantId()))
            .accept(APPLICATION_JSON)
            .method(HttpMethod.POST)
            .body(new FormRequestContent(fields))
            .send(new CompletableFutureResponseListener(future));
        return future.thenApply(cr -> {
           try {
               var json = objectMapper.readTree(new ByteArrayInputStream(cr.getContent()));
               var response = new AzureTokenResponse((ObjectNode) json);
               return new AzureDnsClient(httpClient, response.getAccessToken(), configuration.getSubscription(), configuration.getResourceGroup(), configuration.getZone());
           } catch (Throwable e) {
               throw new CompletionException(e);
           }
        });
    }
}
