package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({MessageReader.Factory.class})
public class WithMetadataMessageReaderFactoryHK2 extends com.exoreaction.xorcery.service.reactivestreams.providers.WithMetadataMessageReaderFactory
        implements MessageReader.Factory {

    @Inject
    public WithMetadataMessageReaderFactoryHK2(Provider<MessageWorkers> messageWorkers) {
        super(messageWorkers::get);
    }

}
