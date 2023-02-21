package com.exoreaction.xorcery.service.reactivestreams.hk2.spi;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers.class})
public class MessageWorkers extends com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers {

    @Inject
    public MessageWorkers(Iterable<MessageWriter.Factory> writers, Iterable<MessageReader.Factory> readers) {
        super(writers, readers);
    }
}
