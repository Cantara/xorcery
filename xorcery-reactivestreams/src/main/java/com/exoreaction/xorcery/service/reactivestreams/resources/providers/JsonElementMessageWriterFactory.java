package com.exoreaction.xorcery.service.reactivestreams.resources.providers;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

@Service
public class JsonElementMessageWriterFactory
        implements MessageWriter.Factory {
    private ObjectMapper objectMapper = new ObjectMapper();

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
