package com.exoreaction.xorcery.service.reactivestreams.hk2.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({MessageWriter.Factory.class})
public class JsonNodeMessageWriterFactory extends com.exoreaction.xorcery.service.reactivestreams.providers.JsonNodeMessageWriterFactory
        implements MessageWriter.Factory {
}
