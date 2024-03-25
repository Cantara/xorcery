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
package com.exoreaction.xorcery.jsonapi.providers;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import static com.exoreaction.xorcery.jsonapi.providers.JsonElementMessageBodyWriter.*;

@Singleton
@Provider
@Produces(MediaType.WILDCARD)
public class JsonMessageBodyWriter
        implements MessageBodyWriter<Object> {
    private final ObjectMapper objectMapper;

    @Inject
    public JsonMessageBodyWriter() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return objectMapper.canSerialize(type)
                && !JsonNode.class.isAssignableFrom(type)
                && !JsonElement.class.isAssignableFrom(type)
                && (mediaType.isCompatible(APPLICATION_JSON_API_TYPE) ||
                mediaType.isCompatible(APPLICATION_JSON_SCHEMA_TYPE) ||
                mediaType.isCompatible(APPLICATION_YAML_TYPE) ||
                mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE) ||
                mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE) ||
                mediaType.isWildcardType());
    }

    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        //        objectMapper.writeValue(entityStream, object);
        String json = objectMapper.writeValueAsString(object);
        entityStream.write(json.getBytes(StandardCharsets.UTF_8));
    }
}
