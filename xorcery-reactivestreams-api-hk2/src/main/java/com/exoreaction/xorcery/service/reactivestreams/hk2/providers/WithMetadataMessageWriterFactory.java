package com.exoreaction.xorcery.service.reactivestreams.hk2.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jvnet.hk2.annotations.Service;

@Service
public class WithMetadataMessageWriterFactory extends com.exoreaction.xorcery.service.reactivestreams.providers.WithMetadataMessageWriterFactory
        implements MessageWriter.Factory {

    @Inject
    public WithMetadataMessageWriterFactory(Provider<MessageWorkers> messageWorkers) {
        super(messageWorkers::get);
    }
}
