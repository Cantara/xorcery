package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@Service
public class JsonElementMessageReaderFactory
        implements MessageReader.Factory {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (JsonElement.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation((Class<JsonElement>) type);
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageReader<JsonElement> {

        private Class<JsonElement> type;

        public MessageWriterImplementation(Class<JsonElement> type) {
            this.type = type;
        }

        @Override
        public JsonElement readFrom(InputStream entityStream) throws IOException {
            try {
                JsonNode json = objectMapper.readTree(entityStream);
                return type.getConstructor(json.getClass()).newInstance(json);
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
