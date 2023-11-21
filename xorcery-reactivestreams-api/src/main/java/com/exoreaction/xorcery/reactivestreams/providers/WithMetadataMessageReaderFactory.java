/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.providers;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public class WithMetadataMessageReaderFactory
        implements MessageReader.Factory {

    private final ObjectMapper jsonMapper;
    private Supplier<MessageWorkers> messageWorkers;

    public WithMetadataMessageReaderFactory(Supplier<MessageWorkers> messageWorkers) {
        jsonMapper = new JsonMapper()
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.messageWorkers = messageWorkers;
    }

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {

        if (type.equals(WithMetadata.class)) {
            if (((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof Class<?> eventType)
            {
                MessageReader<?> eventReader = messageWorkers.get().newReader(eventType, eventType, mediaType);
                if (eventReader != null) {
                    return (MessageReader<T>) new WithMetadataMessageReader<>(eventReader);
                }
            } else if (((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof ParameterizedType parameterizedEventType)
            {
                if (parameterizedEventType.getRawType() instanceof Class<?> eventType)
                {
                    MessageReader<?> eventReader = messageWorkers.get().newReader(eventType, eventType, mediaType);
                    if (eventReader != null) {
                        return (MessageReader<T>) new WithMetadataMessageReader<>(eventReader);
                    }
                }
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
        public WithMetadata<T> readFrom(byte[] bytes, int offset, int len) throws IOException {
            try (JsonParser jp = jsonMapper.createParser(bytes, offset, len))
            {
                Metadata metadata = jp.readValueAs(Metadata.class);
                int metadataOffset = (int)jp.getCurrentLocation().getByteOffset();

                Object event = eventReader.readFrom(bytes, offset+metadataOffset, len-metadataOffset);
                return (WithMetadata<T>) new WithMetadata<>(metadata, event);
            }
        }

        @Override
        public WithMetadata<T> readFrom(InputStream entityStream) throws IOException {
            entityStream.mark(entityStream.available());
            try (JsonParser jp = jsonMapper.createParser(entityStream))
            {
                Metadata metadata = jp.readValueAs(Metadata.class);
                long offset = jp.getCurrentLocation().getByteOffset();

                entityStream.reset();
                entityStream.skip(offset);
                Object event = eventReader.readFrom(entityStream);
                return (WithMetadata<T>) new WithMetadata<>(metadata, event);
            }
        }
    }
}
