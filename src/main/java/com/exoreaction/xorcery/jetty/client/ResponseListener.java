package com.exoreaction.xorcery.jetty.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class ResponseListener extends BufferingResponseListener
{
    private final CompletableFuture<Result> future = new CompletableFuture<>();
    private final Logger log = LogManager.getLogger(ResponseListener.class);

    @Override
    public void onComplete(Result result)
    {
        if (result.isFailed())
        {
            Throwable cause = result.getFailure();
            boolean success = future.completeExceptionally(cause);
            if (!success)
                log.warn("Suppressed exception", cause);
            return;
        }

        result = new Result(result.getRequest(), new HttpContentResponse(result.getResponse(), getContent(), getMediaType(), getEncoding()));

        boolean success = future.complete(result);
        if (!success)
            log.warn("Suppressed result: {}", result);
    }

    /**
     * @return the server response
     */
    public CompletionStage<Result> getResult()
    {
        return future;
    }
}