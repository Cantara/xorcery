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

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class JsonMessageReaderFactory implements MessageReader.Factory {
    private final ObjectMapper jsonMapper;

    public JsonMessageReaderFactory() {
        jsonMapper = new JsonMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public String getContentType(Class<?> type) {
        return "application/json";
    }

    @Override
    public boolean canRead(Class<?> type, String mediaType) {
        return jsonMapper.canDeserialize(jsonMapper.constructType(type))
                && !JsonNode.class.isAssignableFrom(type)
                && !JsonElement.class.isAssignableFrom(type)
                && !WithMetadata.class.isAssignableFrom(type)
                && mediaType.startsWith("application/json");
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
