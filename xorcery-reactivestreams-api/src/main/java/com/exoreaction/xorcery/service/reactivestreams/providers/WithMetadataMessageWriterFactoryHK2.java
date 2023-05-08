package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({MessageWriter.Factory.class})
public class WithMetadataMessageWriterFactoryHK2 extends com.exoreaction.xorcery.service.reactivestreams.providers.WithMetadataMessageWriterFactory
        implements MessageWriter.Factory {

    @Inject
    public WithMetadataMessageWriterFactoryHK2(Provider<MessageWorkers> messageWorkers) {
        super(messageWorkers::get);
    }
}
