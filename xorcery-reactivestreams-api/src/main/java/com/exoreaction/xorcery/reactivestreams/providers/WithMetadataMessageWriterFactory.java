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

import com.exoreaction.xorcery.lang.Classes;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.function.Supplier;

public class WithMetadataMessageWriterFactory
        implements MessageWriter.Factory {

    private final ObjectMapper objectMapper;
    private final ObjectWriter objectWriter;
    private final Supplier<MessageWorkers> messageWorkers;

    public WithMetadataMessageWriterFactory(Supplier<MessageWorkers> messageWorkers) {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectWriter = objectMapper.writer();
        this.messageWorkers = messageWorkers;
    }

    @Override
    public String getContentType(Class<?> type) {
        if (WithMetadata.class.isAssignableFrom(type)) {
            Type parameterType = Classes.resolveActualTypeArgs((Class<? extends WithMetadata<?>>)type, WithMetadata.class)[0];
            if (parameterType instanceof Class<?> eventType)
            {
                return messageWorkers.get().getAvailableWriteContentTypes(eventType, Collections.emptyList())
                        .stream().findFirst().map(ct -> ct+"+metadata").orElse(null);
            } else if (parameterType instanceof ParameterizedType parameterizedEventType)
            {
                if (parameterizedEventType.getRawType() instanceof Class<?> eventType)
                {
                    return messageWorkers.get().getAvailableWriteContentTypes(eventType, Collections.emptyList())
                            .stream().findFirst().map(ct -> ct+"+metadata").orElse(null);
                }
            }
        }
        return null;
    }

    @Override
    public boolean canWrite(Class<?> type, String mediaType) {
        if (WithMetadata.class.isAssignableFrom(type) && mediaType.endsWith("+metadata")) {
            Type parameterType = Classes.resolveActualTypeArgs((Class<? extends WithMetadata<?>>)type, WithMetadata.class)[0];
            String envelopedMetadata = mediaType.substring(0, mediaType.length()-"+metadata".length());
            if (parameterType instanceof Class<?> eventType)
            {
                return messageWorkers.get().canWrite(eventType, envelopedMetadata);
            } else if (parameterType instanceof ParameterizedType parameterizedEventType)
            {
                if (parameterizedEventType.getRawType() instanceof Class<?> eventType)
                {
                    return messageWorkers.get().canWrite(eventType, envelopedMetadata);
                }
            }
        }
        return false;
    }
    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (WithMetadata.class.isAssignableFrom(type)) {
            if (((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof Class<?> eventType)
            {
                MessageWriter<?> eventWriter = messageWorkers.get().newWriter(eventType, eventType, mediaType);
                if (eventWriter != null) {
                    return (MessageWriter<T>) new WithMetadataMessageWriter<>(eventWriter);
                }
            } else if (((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof ParameterizedType parameterizedEventType)
            {
                if (parameterizedEventType.getRawType() instanceof Class<?> eventType)
                {
                    MessageWriter<?> eventWriter = messageWorkers.get().newWriter(eventType, eventType, mediaType);
                    if (eventWriter != null) {
                        return (MessageWriter<T>) new WithMetadataMessageWriter<>(eventWriter);
                    }
                }
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
            objectWriter.writeValue(entityStream, instance.metadata().metadata());
            eventWriter.writeTo(instance.event(), entityStream);
        }
    }
}
