package com.exoreaction.xorcery.service.reactivestreams.resources.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@Service
public class JsonNodeMessageReaderFactory
        implements MessageReader.Factory {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (JsonNode.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageReader<JsonNode> {

        @Override
        public JsonNode readFrom(InputStream entityStream) throws IOException {
            try {
                return objectMapper.readTree(entityStream);
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
