package com.exoreaction.xorcery.service.reactivestreams.resources.providers;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@Service
public class JsonMessageReaderFactory
        implements MessageReader.Factory {
    private final ObjectMapper objectMapper;

    public JsonMessageReaderFactory() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (objectMapper.canDeserialize(objectMapper.constructType(type))
                && !JsonNode.class.isAssignableFrom(type)
                && !WithMetadata.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation((Class<Object>) type);
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageReader<Object> {

        private Class<Object> type;

        public MessageWriterImplementation(Class<Object> type) {
            this.type = type;
        }

        @Override
        public Object readFrom(InputStream entityStream) throws IOException {
            try {
                JsonNode json = objectMapper.readTree(entityStream);
                return type.getConstructor(json.getClass()).newInstance(json);
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
