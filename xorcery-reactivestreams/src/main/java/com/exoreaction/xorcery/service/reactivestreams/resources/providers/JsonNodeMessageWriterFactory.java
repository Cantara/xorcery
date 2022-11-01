package com.exoreaction.xorcery.service.reactivestreams.resources.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

@Service
public class JsonNodeMessageWriterFactory
        implements MessageWriter.Factory {
    private ObjectMapper objectMapper;

    public JsonNodeMessageWriterFactory() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (JsonNode.class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<JsonNode> {
        @Override
        public void writeTo(JsonNode instance, OutputStream out) throws IOException {
            objectMapper.writer().withDefaultPrettyPrinter().writeValue(out, instance);
        }
    }
}
