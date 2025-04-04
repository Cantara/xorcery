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
package dev.xorcery.jsonapi.providers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.xorcery.json.JsonElement;
import dev.xorcery.jsonapi.JsonApiRels;
import dev.xorcery.jsonapi.MediaTypes;
import dev.xorcery.jsonapi.ResourceDocument;
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

@Singleton
@Provider
@Produces({MediaType.WILDCARD})
public class JsonElementMessageBodyWriter
        implements MessageBodyWriter<JsonElement> {

    static final MediaType APPLICATION_JSON_API_TYPE = MediaType.valueOf(MediaTypes.APPLICATION_JSON_API);
    static final MediaType APPLICATION_JSON_SCHEMA_TYPE = MediaType.valueOf(MediaTypes.APPLICATION_JSON_SCHEMA);
    static final MediaType APPLICATION_YAML_TYPE = MediaType.valueOf(MediaTypes.APPLICATION_YAML);

    private final ObjectMapper objectMapper;
    private final ObjectMapper yamlObjectMapper;

    public JsonElementMessageBodyWriter() {
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        this.yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        yamlObjectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonElement.class.isAssignableFrom(type)
                && (mediaType.isCompatible(APPLICATION_JSON_API_TYPE) ||
                mediaType.isCompatible(APPLICATION_JSON_SCHEMA_TYPE) ||
                mediaType.isCompatible(APPLICATION_YAML_TYPE) ||
                mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE) ||
                mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE) ||
                mediaType.isWildcardType());
    }

    @Override
    public void writeTo(JsonElement jsonElement, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        if (jsonElement instanceof ResourceDocument rd) {
            rd.getLinks().getByRel(JsonApiRels.describedby).ifPresent(link ->
            {
                httpHeaders.add("Link", String.format("<%s>; rel=\"%s\"; title=\"%s\"", link.getHref(), link.rel(), "JSON Schema"));
            });
        }

        if (mediaType.isCompatible(APPLICATION_YAML_TYPE)) {
            yamlObjectMapper.writer().writeValue(entityStream, jsonElement.json());
        } else {
            objectMapper.writer().withDefaultPrettyPrinter().writeValue(entityStream, jsonElement.json());
        }
    }
}
