package com.exoreaction.reactiveservices.jaxrs.writers;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
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
import java.util.HashMap;
import java.util.Map;

@Singleton
@Provider
@Produces(MediaTypes.APPLICATION_JSON_API)
public class JsonApiMessageBodyWriter
    implements MessageBodyWriter<ResourceDocument>
{
    private final JsonWriterFactory jsonWriterFactory;

    public JsonApiMessageBodyWriter() {
        Map<String, Object> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        jsonWriterFactory = Json.createWriterFactory(config);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ResourceDocument.class;
    }

    @Override
    public void writeTo(ResourceDocument resourceDocument, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        jsonWriterFactory.createWriter(entityStream, StandardCharsets.UTF_8).write(resourceDocument.json());
    }
}
