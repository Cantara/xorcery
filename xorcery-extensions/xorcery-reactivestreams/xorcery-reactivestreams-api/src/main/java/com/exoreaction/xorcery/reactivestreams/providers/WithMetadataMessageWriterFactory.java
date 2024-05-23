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
import com.exoreaction.xorcery.metadata.WithMetadata;
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
import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

public class WithMetadataMessageWriterFactory
        implements MessageWriter.Factory {

    private final ObjectWriter objectWriter;
    private final Supplier<MessageWorkers> messageWorkersSupplier;

    public WithMetadataMessageWriterFactory(Supplier<MessageWorkers> messageWorkersSupplier) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectWriter = objectMapper.writer();
        this.messageWorkersSupplier = messageWorkersSupplier;
    }

    @Override
    public String getContentType(Class<?> type) {
        if (WithMetadata.class.isAssignableFrom(type)) {
            Type parameterType = Classes.typeOrBound(Classes.resolveActualTypeArgs((Class<? extends WithMetadata<?>>)type, WithMetadata.class)[0]);
            MessageWorkers messageWorkers = messageWorkersSupplier.get();
            if (parameterType instanceof Class<?> eventType)
            {
                Collection<String> availableWriteContentTypes = messageWorkers.getAvailableWriteContentTypes(eventType, Collections.emptyList());
                return availableWriteContentTypes.stream().findFirst().map(ct -> ct+"+metadata").orElse(null);
            } else if (parameterType instanceof ParameterizedType parameterizedEventType)
            {
                if (parameterizedEventType.getRawType() instanceof Class<?> eventType)
                {
                    return messageWorkers.getAvailableWriteContentTypes(eventType, Collections.emptyList())
                            .stream().findFirst().map(ct -> ct+"+metadata").orElse(null);
                }
            }
        }
        return null;
    }

    @Override
    public boolean canWrite(Class<?> type, String mediaType) {
        if (WithMetadata.class.isAssignableFrom(type) && mediaType.endsWith("+metadata")) {
            Type parameterType = Classes.typeOrBound(Classes.resolveActualTypeArgs((Class<? extends WithMetadata<?>>)type, WithMetadata.class)[0]);
            String envelopedContentType = mediaType.substring(0, mediaType.length()-"+metadata".length());
            if (parameterType instanceof Class<?> eventType)
            {
                return messageWorkersSupplier.get().canWrite(eventType, envelopedContentType);
            } else if (parameterType instanceof ParameterizedType parameterizedEventType)
            {
                if (parameterizedEventType.getRawType() instanceof Class<?> eventType)
                {
                    return messageWorkersSupplier.get().canWrite(eventType, envelopedContentType);
                }
            }
        }
        return false;
    }
    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (WithMetadata.class.isAssignableFrom(type) && mediaType.endsWith("+metadata")) {
            Type parameterType = Classes.typeOrBound(Classes.resolveActualTypeArgs((Class<? extends WithMetadata<?>>)type, WithMetadata.class)[0]);
            String envelopedContentType = mediaType.substring(0, mediaType.length()-"+metadata".length());
            if (parameterType instanceof Class<?> eventType)
            {
                MessageWriter<?> eventWriter = messageWorkersSupplier.get().newWriter(eventType, eventType, envelopedContentType);
                if (eventWriter != null) {
                    return (MessageWriter<T>) new WithMetadataMessageWriter<>(eventWriter);
                }
            } else if (parameterType instanceof ParameterizedType parameterizedEventType)
            {
                if (parameterizedEventType.getRawType() instanceof Class<?> eventType)
                {
                    MessageWriter<?> eventWriter = messageWorkersSupplier.get().newWriter(eventType, eventType, envelopedContentType);
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
            try {
                objectWriter.writeValue(entityStream, instance.metadata().metadata());
                eventWriter.writeTo(instance.data(), entityStream);
            } catch (Throwable e) {
                throw new IOException("Failed to write item with metadata:"+instance.metadata().toString(), e);
            }
        }
    }
}
