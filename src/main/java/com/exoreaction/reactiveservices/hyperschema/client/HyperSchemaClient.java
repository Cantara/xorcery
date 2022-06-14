package com.exoreaction.reactiveservices.hyperschema.client;

import com.exoreaction.reactiveservices.hyperschema.model.HyperSchema;
import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonschema.model.JsonSchema;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.InvocationCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public record HyperSchemaClient(Client client) {

    public CompletionStage<HyperSchema> get(Link link) {
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
