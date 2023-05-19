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
package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class JsonMessageReaderFactory
        implements MessageReader.Factory {
    private final ObjectMapper objectMapper;

    public JsonMessageReaderFactory() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (objectMapper.canDeserialize(objectMapper.constructType(type))
                && !JsonNode.class.isAssignableFrom(type)
                && !WithMetadata.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation((Class<Object>) type);
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageReader<Object> {

        private Class<Object> type;

        public MessageWriterImplementation(Class<Object> type) {
            this.type = type;
        }

        @Override
        public Object readFrom(InputStream entityStream) throws IOException {
            try {
                return objectMapper.readValue(entityStream, type);
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
