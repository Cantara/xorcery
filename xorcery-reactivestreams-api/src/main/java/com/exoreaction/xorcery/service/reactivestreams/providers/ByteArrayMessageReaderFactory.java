package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class ByteArrayMessageReaderFactory
        implements MessageReader.Factory {

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (byte[].class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation();
        else
            return null;
    }

    static class MessageWriterImplementation
            implements MessageReader<byte[]> {

        @Override
        public byte[] readFrom(InputStream entityStream) throws IOException {
            return entityStream.readAllBytes();
        }
    }
}
