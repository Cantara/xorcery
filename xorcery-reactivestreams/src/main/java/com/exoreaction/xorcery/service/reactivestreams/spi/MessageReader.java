package com.exoreaction.xorcery.service.reactivestreams.spi;

import org.jvnet.hk2.annotations.Contract;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public interface MessageReader<T> {

    @Contract
    interface Factory {
        <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType);
    }

    T readFrom(ByteBuffer buffer) throws java.io.IOException;
}
