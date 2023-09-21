package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletableFuture;

@Service(name="testerrorsubscriber")
@RunLevel(19)
public class TestErrorSubscriber
    implements PersistentSubscriber
{
    @Inject
    public TestErrorSubscriber(Logger logger) {
        logger.info("Test error subscriber started");
    }

    @Override
    public void handle(WithMetadata<ArrayNode> eventsWithMetadata, CompletableFuture<Void> result) {
        System.out.println(eventsWithMetadata.metadata().json().get("revision"));
        result.completeExceptionally(new IllegalArgumentException("Something went wrong"));
    }
}
