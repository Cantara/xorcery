package com.exoreaction.xorcery.service.reactivestreams.hk2.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.jvnet.hk2.annotations.Service;

@Service
public class JsonElementMessageWriterFactory extends com.exoreaction.xorcery.service.reactivestreams.providers.JsonElementMessageWriterFactory
        implements MessageWriter.Factory {

}
