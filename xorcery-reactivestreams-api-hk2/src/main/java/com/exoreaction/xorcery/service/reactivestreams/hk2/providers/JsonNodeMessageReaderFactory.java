package com.exoreaction.xorcery.service.reactivestreams.hk2.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({MessageReader.Factory.class})
public class JsonNodeMessageReaderFactory extends com.exoreaction.xorcery.service.reactivestreams.providers.JsonNodeMessageReaderFactory
        implements MessageReader.Factory {
}
