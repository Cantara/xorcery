package com.exoreaction.xorcery.rest;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Result;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface RestHelpers {
    ObjectMapper defaultMapper = new ObjectMapper();

    static CompletionStage<ResourceDocument> toResourceDocument(Result result) {
        CompletableFuture<ResourceDocument> future = new CompletableFuture<>();

        new ResultHandler() {
            @Override
            public void status200(Result result) {
                String content = ((ContentResponse) result.getResponse()).getContentAsString();
                try {
                    future.complete(new ResourceDocument((ObjectNode) defaultMapper.readTree(content)));
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void error(Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }.accept(result);
        return future;
    }

}
