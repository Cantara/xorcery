package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi;

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.PersistentSubscriberConfiguration;
import org.jvnet.hk2.annotations.Contract;

import java.io.Closeable;
import java.io.IOException;

@Contract
public interface PersistentSubscriberCheckpoint
    extends Closeable
{
    void init(PersistentSubscriberConfiguration configuration) throws IOException;

    long getCheckpoint() throws IOException;
    void setCheckpoint(long revision) throws IOException;
}
