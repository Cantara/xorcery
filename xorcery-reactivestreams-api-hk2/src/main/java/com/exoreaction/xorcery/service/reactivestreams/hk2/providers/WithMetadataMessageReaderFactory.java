package com.exoreaction.xorcery.service.reactivestreams.hk2.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jvnet.hk2.annotations.Service;

@Service
public class WithMetadataMessageReaderFactory extends com.exoreaction.xorcery.service.reactivestreams.providers.WithMetadataMessageReaderFactory
        implements MessageReader.Factory {

    @Inject
    public WithMetadataMessageReaderFactory(Provider<MessageWorkers> messageWorkers) {
        super(messageWorkers::get);
    }

}
