package com.exoreaction.reactiveservices.jsonapi.client;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public record JsonApiClient(Client client) {

    public CompletionStage<ResourceDocument> get(Link link)
    {
        CompletableFuture<ResourceDocument> future = new CompletableFuture<>();
        client.target(link.getHrefAsUri())
                .request(MediaTypes.APPLICATION_JSON_API_TYPE)
                .buildGet()
                .submit(new InvocationCallback<ResourceDocument>() {
                    @Override
                    public void completed(ResourceDocument resourceDocument) {
                        future.complete(resourceDocument);
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                });
        return future;
    }

    public CompletionStage<ResourceDocument> submit(Link link, ResourceDocument resourceDocument) {
        CompletableFuture<ResourceDocument> future = new CompletableFuture<>();
        client.target(link.getHrefAsUri())
                .request(MediaTypes.APPLICATION_JSON_API_TYPE)
                .buildPost(Entity.entity(resourceDocument.json(), MediaTypes.APPLICATION_JSON_API_TYPE))
                .submit(new InvocationCallback<ResourceDocument>() {
                    @Override
                    public void completed(ResourceDocument resourceDocument) {
                        future.complete(resourceDocument);
                    }

                    @Override
                    public void failed(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                });
        return future;
    }
}
