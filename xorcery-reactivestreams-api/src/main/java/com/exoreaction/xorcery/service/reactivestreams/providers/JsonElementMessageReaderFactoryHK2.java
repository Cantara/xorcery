package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({MessageReader.Factory.class})
public class JsonElementMessageReaderFactoryHK2 extends com.exoreaction.xorcery.service.reactivestreams.providers.JsonElementMessageReaderFactory
        implements MessageReader.Factory {
}
