package com.exoreaction.xorcery.rest;

import com.exoreaction.xorcery.jaxrs.MediaTypes;
import com.exoreaction.xorcery.jetty.client.ResponseListener;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BytesRequestContent;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class RestClient {
    private final HttpClient httpClient;
    private final WebSocketClient webSocketClient;

    public RestClient(WebSocketClient webSocketClient) {
        this.httpClient = webSocketClient.getHttpClient();
        this.webSocketClient = webSocketClient;
    }

    public CompletionStage<Result> get(String resourceUri) {
        ResponseListener response = new ResponseListener();
        httpClient.newRequest(resourceUri).method(HttpMethod.GET).send(response);
        return response.getResult();
    }

    public CompletionStage<Session> connect(String uri, Object clientEndpoint) {

        try {
            return webSocketClient.connect(clientEndpoint, URI.create(uri));
        } catch (IOException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    public CompletionStage<Result> submit(String uri, ResourceDocument resourceDocument) {
        ResponseListener response = new ResponseListener();
        Request.Content content = new BytesRequestContent(MediaTypes.APPLICATION_JSON_API, resourceDocument.toString().getBytes(StandardCharsets.UTF_8));
        httpClient.newRequest(uri).method(HttpMethod.POST).body(content).send(response);
        return response.getResult();
    }
}
