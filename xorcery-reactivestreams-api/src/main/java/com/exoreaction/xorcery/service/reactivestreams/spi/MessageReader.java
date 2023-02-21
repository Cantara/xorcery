package com.exoreaction.xorcery.service.reactivestreams.spi;

import java.io.InputStream;
import java.lang.reflect.Type;

public interface MessageReader<T> {

    interface Factory {
        <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType);
    }

    T readFrom(InputStream in) throws java.io.IOException;
}
