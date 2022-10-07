package com.exoreaction.xorcery.jsonapi.jaxrs.providers;

import com.exoreaction.xorcery.jsonapi.jaxrs.MediaTypes;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Singleton
@Provider
@Consumes(MediaTypes.APPLICATION_JSON_API)
public class JsonElementMessageBodyReader
        implements MessageBodyReader<JsonElement> {

    private final ObjectMapper objectMapper;

    @Inject
    public JsonElementMessageBodyReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return JsonElement.class.isAssignableFrom(type);
    }

    @Override
    public JsonElement readFrom(Class<JsonElement> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            JsonNode json = objectMapper.readTree(entityStream);
            return type.getConstructor(json.getClass()).newInstance(json);
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }
}
