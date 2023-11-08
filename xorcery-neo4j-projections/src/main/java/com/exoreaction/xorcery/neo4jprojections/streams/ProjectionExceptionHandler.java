package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.ExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.reactivestreams.Subscription;

public class ProjectionExceptionHandler implements ExceptionHandler<WithMetadata<ArrayNode>> {
    private final Subscription subscription;

    public ProjectionExceptionHandler(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, WithMetadata<ArrayNode> event) {
        subscription.cancel();
        LogManager.getLogger(getClass()).error("Cancelled subscription", ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        subscription.cancel();
        LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        subscription.cancel();
        LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
    }
}
