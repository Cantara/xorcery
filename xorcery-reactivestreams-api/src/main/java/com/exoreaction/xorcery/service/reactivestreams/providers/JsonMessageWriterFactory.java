package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class JsonMessageWriterFactory
        implements MessageWriter.Factory {
    private final ObjectMapper objectMapper;

    public JsonMessageWriterFactory() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (objectMapper.canSerialize(type)
                && !JsonNode.class.isAssignableFrom(type)
                && !WithMetadata.class.isAssignableFrom(type))
        {
            return (MessageWriter<T>) new MessageWriterImplementation();
        } else
        {
            return null;
        }
    }

    class MessageWriterImplementation
            implements MessageWriter<Object> {
        @Override
        public void writeTo(Object instance, OutputStream out) throws IOException {
            objectMapper.writer().withDefaultPrettyPrinter().writeValue(out, instance);
        }
    }
}
