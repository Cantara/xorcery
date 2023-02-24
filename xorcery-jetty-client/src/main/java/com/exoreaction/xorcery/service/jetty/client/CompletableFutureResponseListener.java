package com.exoreaction.xorcery.service.jetty.client;

import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;

import java.util.concurrent.CompletableFuture;

public class CompletableFutureResponseListener extends BufferingResponseListener {
    private CompletableFuture<ContentResponse> future;

    public CompletableFutureResponseListener(CompletableFuture<ContentResponse> future, int maxLength) {
        super(maxLength);
        this.future = future;
    }

    public CompletableFutureResponseListener(CompletableFuture<ContentResponse> future) {
        this(future, 2 * 1024 * 1024);
    }

    @Override
    public void onComplete(Result result) {

        if (result.isFailed()) {
            future.completeExceptionally(result.getFailure());
        } else {
            future.complete(new HttpContentResponse(result.getResponse(), getContent(), getMediaType(), getEncoding()));
        }
    }
}
