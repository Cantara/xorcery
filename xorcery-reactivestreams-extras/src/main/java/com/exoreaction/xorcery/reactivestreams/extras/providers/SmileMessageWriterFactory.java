package com.exoreaction.xorcery.reactivestreams.extras.providers;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

@Service
@ContractsProvided({MessageWriter.Factory.class})
public class SmileMessageWriterFactory
        implements MessageWriter.Factory {
    private final ObjectMapper jsonMapper;

    public SmileMessageWriterFactory() {
        jsonMapper = new SmileMapper()
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getContentType(Class<?> type) {
        return "application/x-jackson-smile";
    }

    @Override
    public boolean canWrite(Class<?> type, String mediaType) {
        return jsonMapper.canDeserialize(jsonMapper.constructType(type))
                && !JsonNode.class.isAssignableFrom(type)
                && !WithMetadata.class.isAssignableFrom(type)
                && mediaType.equals("application/x-jackson-smile");
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (canWrite(type, mediaType)) {
            return (MessageWriter<T>) new SmileMessageWriter();
        } else {
            return null;
        }
    }

    class SmileMessageWriter
            implements MessageWriter<Object> {
        @Override
        public void writeTo(Object instance, OutputStream out) throws IOException {
            try (JsonGenerator generator = jsonMapper.createGenerator(out)) {
                generator.writeObject(instance);
            }
        }
    }
}
