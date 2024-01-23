package com.exoreaction.xorcery.eventstore;

import com.eventstore.dbclient.NotLeaderException;
import com.exoreaction.xorcery.process.Process;

public interface EventStoreClientProcess<T>
    extends Process<T>
{
    @Override
    default boolean isRetryable(Throwable t) {
        return t instanceof NotLeaderException;
    }
}
