package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.domainevents.api.CommandEvents;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lmax.disruptor.ExceptionHandler;
import org.apache.logging.log4j.LogManager;
import org.neo4j.exceptions.EntityNotFoundException;
import org.reactivestreams.Subscription;

public class ProjectionExceptionHandler implements ExceptionHandler<CommandEvents> {
    private final Subscription subscription;

    public ProjectionExceptionHandler(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, CommandEvents event) {
        LogManager.getLogger(getClass()).error("Cancelled subscription", ex);
        try {
            subscription.cancel();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Failed to cancel subscription", ex);
        }
        throw new RuntimeException("Projection cancelled", ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
        try {
            subscription.cancel();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Failed to cancel subscription", ex);
        }
        throw new RuntimeException("Projection cancelled", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        LogManager.getLogger(getClass()).warn("Cancelled subscription", ex);
        try {
            subscription.cancel();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Failed to cancel subscription", ex);
        }
    }
}
