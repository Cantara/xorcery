package com.exoreaction.xorcery.service.reactivestreams.spi;

import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

public interface MessageWriter<T> {

    @Contract
    interface Factory {
        <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType);
    }

    void writeTo(T instance, OutputStream out) throws IOException;
}
