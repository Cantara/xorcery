package com.exoreaction.xorcery.service.reactivestreams.test.media;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

@Service
public class LongMessageReaderFactory
        implements MessageReader.Factory {

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (Long.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation();
        else
            return null;
    }

    static class MessageWriterImplementation
            implements MessageReader<Long> {

        @Override
        public Long readFrom(InputStream entityStream) throws IOException {
            byte[] buf = new byte[8];
            int read = entityStream.read(buf);
            if (read != 8) {
                throw new IOException("Only " + read + " bytes read, should have been 8");
            }
            Long item = ByteBuffer.wrap(buf).getLong();
            return item;
        }
    }
}
