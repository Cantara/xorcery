package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public class ByteBufferMessageReaderFactory
        implements MessageReader.Factory {

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (ByteBuffer.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation();
        else
            return null;
    }

    static class MessageWriterImplementation
            implements MessageReader<ByteBuffer> {

        @Override
        public ByteBuffer readFrom(InputStream entityStream) throws IOException {
            return ByteBuffer.wrap(entityStream.readAllBytes());
        }
    }
}
