package com.exoreaction.reactiveservices.jaxrs.writers;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
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

@Singleton
@Provider
@Produces(MediaTypes.APPLICATION_JSON_API)
public class JsonApiMessageBodyWriter
    implements MessageBodyWriter<ResourceDocument>
{
    private final ObjectMapper objectMapper;

    @Inject
    public JsonApiMessageBodyWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ResourceDocument.class;
    }

    @Override
    public void writeTo(ResourceDocument resourceDocument, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        objectMapper.writer().withDefaultPrettyPrinter().writeValue(entityStream, resourceDocument.json());
    }
}
