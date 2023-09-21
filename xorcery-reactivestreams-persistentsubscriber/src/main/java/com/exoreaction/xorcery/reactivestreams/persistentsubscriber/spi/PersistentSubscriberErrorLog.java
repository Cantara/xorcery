package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi;

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.PersistentSubscriberConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jvnet.hk2.annotations.Contract;

import java.io.Closeable;
import java.io.IOException;

@Contract
public interface PersistentSubscriberErrorLog
        extends Closeable {

    public void init(PersistentSubscriberConfiguration configuration, PersistentSubscriber persistentSubscriber) throws IOException;

    public void handle(WithMetadata<ArrayNode> arrayNodeWithMetadata, Throwable exception) throws IOException;
}
