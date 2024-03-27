package com.exoreaction.xorcery.reactivestreams.extras.providers;

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

@Service
@ContractsProvided({MessageReader.Factory.class})
public class SmileMessageReaderFactory
        implements MessageReader.Factory {
    private final ObjectMapper jsonMapper;

    public SmileMessageReaderFactory() {
        jsonMapper = new SmileMapper()
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getContentType(Class<?> type) {
        return "application/x-jackson-smile";
    }

    @Override
    public boolean canRead(Class<?> type, String mediaType) {
        return jsonMapper.canDeserialize(jsonMapper.constructType(type))
                && !JsonNode.class.isAssignableFrom(type)
                && !WithMetadata.class.isAssignableFrom(type)
                && mediaType.equals("application/x-jackson-smile");
    }

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (canRead(type, mediaType))
            return (MessageReader<T>) new JsonMessageReader((Class<Object>) type);
        else return null;
    }

    class JsonMessageReader implements MessageReader<Object> {

        private Class<Object> type;

        public JsonMessageReader(Class<Object> type) {
            this.type = type;
        }

        @Override
        public Object readFrom(byte[] bytes, int offset, int len) throws IOException {
            try (JsonParser jp = jsonMapper.createParser(bytes, offset, len)) {
                return jp.readValueAs(type);
            }
        }

        @Override
        public Object readFrom(InputStream entityStream) throws IOException {
            try (JsonParser jp = jsonMapper.createParser(entityStream)) {
                return jp.readValueAs(type);
            }
        }
    }
}
