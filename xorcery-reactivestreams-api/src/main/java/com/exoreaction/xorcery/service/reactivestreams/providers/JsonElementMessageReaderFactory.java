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

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class JsonElementMessageReaderFactory
        implements MessageReader.Factory {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (JsonElement.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation((Class<JsonElement>) type);
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageReader<JsonElement> {

        private Class<JsonElement> type;

        public MessageWriterImplementation(Class<JsonElement> type) {
            this.type = type;
        }

        @Override
        public JsonElement readFrom(InputStream entityStream) throws IOException {
            try {
                JsonNode json = objectMapper.readTree(entityStream);
                return type.getConstructor(json.getClass()).newInstance(json);
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
