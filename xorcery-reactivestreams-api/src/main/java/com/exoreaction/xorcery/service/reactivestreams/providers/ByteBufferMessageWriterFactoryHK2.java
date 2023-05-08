package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({MessageWriter.Factory.class})
public class ByteBufferMessageWriterFactoryHK2 extends com.exoreaction.xorcery.service.reactivestreams.providers.ByteBufferMessageWriterFactory
        implements MessageWriter.Factory {

}
