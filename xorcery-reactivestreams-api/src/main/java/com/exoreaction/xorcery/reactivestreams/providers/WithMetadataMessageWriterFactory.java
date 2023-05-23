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

import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public class WithMetadataMessageWriterFactory
        implements MessageWriter.Factory {

    private final ObjectMapper objectMapper;
    private Supplier<MessageWorkers> messageWorkers;

    public WithMetadataMessageWriterFactory(Supplier<MessageWorkers> messageWorkers) {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.messageWorkers = messageWorkers;
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (type.equals(WithMetadata.class)) {
            Class<?> eventType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
            MessageWriter<?> eventWriter = messageWorkers.get().newWriter(eventType, eventType, mediaType);
            if (eventWriter != null) {
                return (MessageWriter<T>) new WithMetadataMessageWriter<>(eventWriter);
            }
        }
        return null;
    }

    class WithMetadataMessageWriter<T>
            implements MessageWriter<WithMetadata<T>> {

        private final MessageWriter<T> eventWriter;

        public WithMetadataMessageWriter(MessageWriter<T> eventReader) {

            this.eventWriter = eventReader;
        }

        @Override
        public void writeTo(WithMetadata<T> instance, OutputStream entityStream) throws IOException {
            objectMapper.writeValue(entityStream, instance.metadata().metadata());
            eventWriter.writeTo(instance.event(), entityStream);
        }
    }
}
