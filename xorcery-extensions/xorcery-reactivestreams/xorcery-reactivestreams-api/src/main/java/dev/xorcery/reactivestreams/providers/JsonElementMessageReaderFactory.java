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
package dev.xorcery.reactivestreams.providers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.json.JsonElement;
import dev.xorcery.reactivestreams.spi.MessageReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class JsonElementMessageReaderFactory
        implements MessageReader.Factory {
    private final ObjectMapper jsonMapper;

    public JsonElementMessageReaderFactory() {
        jsonMapper = new ObjectMapper()
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules();
    }

    @Override
    public String getContentType(Class<?> type) {
        return "application/json";
    }

    @Override
    public boolean canRead(Class<?> type, String mediaType) {
        return JsonElement.class.isAssignableFrom(type) && mediaType.startsWith("application/json");
    }

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (canRead(type, mediaType))
            return (MessageReader<T>) new JsonElementMessageReader((Class<JsonElement>) type);
        else
            return null;
    }

    class JsonElementMessageReader
            implements MessageReader<JsonElement> {

        private Class<JsonElement> type;

        public JsonElementMessageReader(Class<JsonElement> type) {
            this.type = type;
        }

        public JsonElement readFrom(byte[] bytes, int offset, int len)
                throws IOException {
            try (JsonParser jp = jsonMapper.createParser(bytes, offset, len)) {
                JsonNode json = jp.readValueAsTree();
                return type.getConstructor(json.getClass()).newInstance(json);
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }

        @Override
        public JsonElement readFrom(InputStream entityStream) throws IOException {
            try (JsonParser jp = jsonMapper.createParser(entityStream)) {
                JsonNode json = jp.readValueAsTree();
                return type.getConstructor(json.getClass()).newInstance(json);
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
