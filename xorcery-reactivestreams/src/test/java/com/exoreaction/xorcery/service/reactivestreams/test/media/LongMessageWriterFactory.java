package com.exoreaction.xorcery.service.reactivestreams.test.media;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

@Service
public class LongMessageWriterFactory
        implements MessageWriter.Factory {

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (Long.class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<Long> {
        @Override
        public void writeTo(Long instance, OutputStream out) throws IOException {
            byte[] buf = new byte[8];
            ByteBuffer.wrap(buf).putLong(instance);
            out.write(buf);
        }
    }
}
