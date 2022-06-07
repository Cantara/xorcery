package com.exoreaction.reactiveservices.jaxrs.readers;

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
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
public class JsonMessageBodyReader
        implements MessageBodyReader<Object> {
    private final ObjectMapper objectMapper;

    @Inject
    public JsonMessageBodyReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return objectMapper.canDeserialize(objectMapper.constructType(type)) && !JsonNode.class.isAssignableFrom(type);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {

        return objectMapper.readValue(entityStream, type);
    }
}
