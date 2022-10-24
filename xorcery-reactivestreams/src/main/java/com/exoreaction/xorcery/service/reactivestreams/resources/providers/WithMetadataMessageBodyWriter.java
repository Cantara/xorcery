package com.exoreaction.xorcery.service.reactivestreams.resources.providers;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.util.Classes;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.message.MessageBodyWorkers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Provider
@Produces(MediaType.WILDCARD)
public class WithMetadataMessageBodyWriter
        implements MessageBodyWriter<WithMetadata<Object>> {
    private final ObjectMapper objectMapper;
    private MessageBodyWorkers messageBodyWorkers;
    private MessageBodyWriter<Object> entityWriter;

    @Inject
    public WithMetadataMessageBodyWriter(MessageBodyWorkers messageBodyWorkers) {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.messageBodyWorkers = messageBodyWorkers;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (type.equals(WithMetadata.class)) {
            Type eventType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            entityWriter = messageBodyWorkers.getMessageBodyWriter(Classes.getClass(eventType), eventType, new Annotation[0], MediaType.WILDCARD_TYPE);
            return entityWriter != null;
        }
        return false;
    }

    @Override
    public void writeTo(WithMetadata<Object> withMetadata, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        Type eventType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        Class<Object> eventClass = Classes.getClass(eventType);

        MessageBodyWriter<Object> writer = messageBodyWorkers.getMessageBodyWriter(eventClass, eventType, new Annotation[0], MediaType.WILDCARD_TYPE);

        objectMapper.writeValue(entityStream, withMetadata.metadata().metadata());

        writer.writeTo(withMetadata.event(), eventClass, eventType, new Annotation[0], MediaType.WILDCARD_TYPE, new MultivaluedHashMap<>(), entityStream);
    }
}
