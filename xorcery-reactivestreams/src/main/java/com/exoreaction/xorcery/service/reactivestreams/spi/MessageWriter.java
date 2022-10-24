package com.exoreaction.xorcery.service.reactivestreams.spi;

import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public interface MessageWriter<T> {

    @Contract
    interface Factory {
        <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType);
    }

    void writeTo(T instance, ByteBuffer buffer) throws IOException;
}
