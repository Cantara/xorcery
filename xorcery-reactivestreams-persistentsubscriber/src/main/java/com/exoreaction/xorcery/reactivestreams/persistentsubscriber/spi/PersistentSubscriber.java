package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

@Contract
public interface PersistentSubscriber {

    default void init(PersistentSubscriberConfiguration subscriberConfiguration)
            throws IOException
    {
    }

    default Predicate<WithMetadata<ArrayNode>> getFilter() {
        return wman -> true;
    }

    void handle(WithMetadata<ArrayNode> eventsWithMetadata, CompletableFuture<Void> result);
}
