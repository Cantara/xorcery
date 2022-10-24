package com.exoreaction.xorcery.jsonapi.jaxrs.providers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
        return objectMapper.canSerialize(type) && mediaType.isCompatible(MediaType.APPLICATION_OCTET_STREAM_TYPE);
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
