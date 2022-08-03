package com.exoreaction.xorcery.service.opensearch.client;

import com.exoreaction.xorcery.jaxrs.readers.JsonNodeMessageBodyReader;
import com.exoreaction.xorcery.jaxrs.writers.ObjectNodeMessageBodyWriter;
import com.exoreaction.xorcery.service.opensearch.client.document.DocumentClient;
import com.exoreaction.xorcery.service.opensearch.client.index.IndexClient;
import com.exoreaction.xorcery.service.opensearch.client.jaxrs.BulkRequestMessageBodyWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import org.glassfish.jersey.client.ClientConfig;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

public record OpenSearchClient(Client client, URI host) {

    public OpenSearchClient(ClientConfig clientConfig, URI host) {
        this(ClientBuilder.newBuilder().withConfig(clientConfig
                        .register(new ObjectNodeMessageBodyWriter(new ObjectMapper()))
                        .register(new JsonNodeMessageBodyReader(new ObjectMapper()))
                        .register(new BulkRequestMessageBodyWriter(new ObjectMapper()
                                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false))))
                .build(), host);
    }

    public IndexClient indices() {
        return new IndexClient(this::call);
    }

    public DocumentClient documents() {
        return new DocumentClient(this::call);
    }

    private WebTarget request() {
        return client.target(host);
    }

    private CompletionStage<ObjectNode> call(BiConsumer<WebTarget, InvocationCallback<ObjectNode>> invocation)
    {
        OpenSearchRestProcess restProcess = new OpenSearchRestProcess(this::request, new CompletableFuture<>(), invocation);
        restProcess.start();
        return restProcess.result();
    }
}
