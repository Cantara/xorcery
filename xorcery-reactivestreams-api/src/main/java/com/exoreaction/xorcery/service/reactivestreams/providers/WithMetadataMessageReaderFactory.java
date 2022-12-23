package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Service
public class WithMetadataMessageReaderFactory
        implements MessageReader.Factory {

    private final ObjectMapper objectMapper;
    private Provider<MessageWorkers> messageWorkers;

    @Inject
    public WithMetadataMessageReaderFactory(Provider<MessageWorkers> messageWorkers) {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.messageWorkers = messageWorkers;
    }

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {

        if (type.equals(WithMetadata.class)) {
            Class<?> eventType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
            MessageReader<?> eventReader = messageWorkers.get().newReader(eventType, eventType, mediaType);
            if (eventReader != null) {
                return (MessageReader<T>) new WithMetadataMessageReader<>(eventReader);
            }
        }
        return null;
    }

    class WithMetadataMessageReader<T>
            implements MessageReader<WithMetadata<T>> {

        private MessageReader<?> eventReader;

        public WithMetadataMessageReader(MessageReader<?> eventReader) {

            this.eventReader = eventReader;
        }

        @Override
        public WithMetadata<T> readFrom(InputStream entityStream) throws IOException {
            entityStream.mark(entityStream.available());
            JsonFactory jf = new JsonFactory(objectMapper);
            JsonParser jp = jf.createParser(entityStream);
            Metadata metadata = jp.readValueAs(Metadata.class);
            long offset = jp.getCurrentLocation().getByteOffset();

            entityStream.reset();
            entityStream.skip(offset);

            Object event = eventReader.readFrom(entityStream);
            return (WithMetadata<T>) new WithMetadata<>(metadata, event);
        }
    }
}
