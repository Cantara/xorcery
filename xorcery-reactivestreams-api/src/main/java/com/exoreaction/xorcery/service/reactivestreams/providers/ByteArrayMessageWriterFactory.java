package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class ByteArrayMessageWriterFactory
        implements MessageWriter.Factory {

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (byte[].class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<byte[]> {
        @Override
        public void writeTo(byte[] instance, OutputStream out) throws IOException {
            out.write(instance);
        }
    }
}
