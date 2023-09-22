package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberConfiguration;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.filter.SkipUntilTimestampFilter;
import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers.BasePersistentSubscriber;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

@Service(name="testsubscriber")
@RunLevel(19)
public class TestSubscriber
    extends BasePersistentSubscriber
{
    public static AtomicLong handled = new AtomicLong();

    @Inject
    public TestSubscriber(Logger logger) {
        logger.info("Test subscriber started");
        handled.set(0);
    }

    @Override
    public void handle(WithMetadata<ArrayNode> eventsWithMetadata, CompletableFuture<Void> result) {
        System.out.println(eventsWithMetadata.metadata().json().get("revision"));
        handled.incrementAndGet();
        result.complete(null);
    }
}
