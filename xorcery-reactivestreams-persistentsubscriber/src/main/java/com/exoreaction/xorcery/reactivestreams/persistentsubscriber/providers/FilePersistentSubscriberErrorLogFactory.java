package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.reactivestreams.persistentsubscriber.spi.PersistentSubscriberErrorLog;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class FilePersistentSubscriberErrorLogFactory
        implements Factory<PersistentSubscriberErrorLog> {
    private final LoggerContext loggerContext;

    @Inject
    public FilePersistentSubscriberErrorLogFactory(LoggerContext loggerContext) {
        this.loggerContext = loggerContext;
    }

    @Override
    @Named("file")
    public PersistentSubscriberErrorLog provide() {
        return new FilePersistentSubscriberErrorLog(loggerContext.getLogger(FilePersistentSubscriberErrorLog.class));
    }

    @Override
    public void dispose(PersistentSubscriberErrorLog instance) {

    }
}
