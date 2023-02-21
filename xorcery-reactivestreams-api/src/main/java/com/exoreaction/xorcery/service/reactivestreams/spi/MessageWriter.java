package com.exoreaction.xorcery.service.reactivestreams.spi;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

public interface MessageWriter<T> {

    interface Factory {
        <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType);
    }

    void writeTo(T instance, OutputStream out) throws IOException;
}
