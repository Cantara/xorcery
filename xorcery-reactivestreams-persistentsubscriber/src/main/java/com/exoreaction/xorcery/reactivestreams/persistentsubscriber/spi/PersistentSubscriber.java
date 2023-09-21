package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jvnet.hk2.annotations.Contract;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Contract
public interface PersistentSubscriber {
    void handle(WithMetadata<ArrayNode> eventsWithMetadata, CompletableFuture<Void> result);
}
