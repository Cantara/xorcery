package com.exoreaction.xorcery.service.reactivestreams.hk2.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import org.jvnet.hk2.annotations.Service;

@Service
public class JsonElementMessageReaderFactory extends com.exoreaction.xorcery.service.reactivestreams.providers.JsonElementMessageReaderFactory
        implements MessageReader.Factory {
}
