package com.exoreaction.xorcery.service.log4jpublisher.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;
import org.apache.logging.log4j.core.parser.ParseException;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

@Service
public class LogEventMessageReaderFactory
        implements MessageReader.Factory {

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (LogEvent.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageReader<LogEvent> {

        private final JsonLogEventParser parser= new JsonLogEventParser();;

        @Override
        public LogEvent readFrom(InputStream entityStream) throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(entityStream.available());
            Channels.newChannel(entityStream).read(byteBuffer);
            try {
                return parser.parseFrom(byteBuffer.array());
            } catch (ParseException e) {
                throw new IOException(e);
            }
        }
    }
}
