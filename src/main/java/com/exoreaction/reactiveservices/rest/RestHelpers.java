package com.exoreaction.reactiveservices.rest;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import jakarta.json.Json;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Result;

import java.io.StringReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface RestHelpers
{
    static CompletionStage<ResourceDocument> toResourceDocument(Result result)
    {
        CompletableFuture<ResourceDocument> future = new CompletableFuture<>();

        new ResultHandler()
        {
            @Override
            public void status200(Result result) {
                String content = ((ContentResponse) result.getResponse()).getContentAsString();
                future.complete(new ResourceDocument(Json.createReader(new StringReader(content)).readObject()));
            }

            @Override
            public void error(Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }.accept(result);
        return future;
    }

}
