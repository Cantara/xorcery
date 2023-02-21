package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class JsonElementMessageWriterFactory
        implements MessageWriter.Factory {
    private ObjectMapper objectMapper;

    public JsonElementMessageWriterFactory() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (JsonElement.class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<JsonElement> {
        @Override
        public void writeTo(JsonElement instance, OutputStream out) throws IOException {
            objectMapper.writer().withDefaultPrettyPrinter().writeValue(out, instance.json());
        }
    }
}
