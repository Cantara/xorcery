package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberCheckpoint;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class FilePersistentSubscriberCheckpointFactory
    implements Factory<PersistentSubscriberCheckpoint>
{
    private final LoggerContext loggerContext;

    @Inject
    public FilePersistentSubscriberCheckpointFactory(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    @Named("file")
    public PersistentSubscriberCheckpoint provide() {
        return new FilePersistentSubscriberCheckpoint(loggerContext.getLogger(FilePersistentSubscriberCheckpoint.class));
    }

    @Override
    public void dispose(PersistentSubscriberCheckpoint instance) {

    }
}
