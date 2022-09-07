package com.exoreaction.xorcery.service.reactivestreams.resources.providers;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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

@Singleton
@Provider
@Produces({MediaType.WILDCARD})
public class WithMetadataMessageBodyWriter
        implements MessageBodyWriter<WithMetadata<Object>> {
    private final ObjectMapper objectMapper;
    private MessageBodyWorkers messageBodyWorkers;

    @Inject
    public WithMetadataMessageBodyWriter(ObjectMapper objectMapper, MessageBodyWorkers messageBodyWorkers) {
        this.objectMapper = objectMapper;
        this.messageBodyWorkers = messageBodyWorkers;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (type.equals(WithMetadata.class)) {
            Class<?> eventType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[1];
            return !messageBodyWorkers.getMessageBodyWritersForType(eventType).isEmpty();
        }
        return false;
    }

    @Override
    public void writeTo(WithMetadata<Object> withMetadata, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        Type eventType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
        Class<?> eventClass = (Class<?>) eventType;
        MessageBodyWriter writer = messageBodyWorkers.getMessageBodyWritersForType(eventClass).get(0);

        objectMapper.writeValue(entityStream, withMetadata.metadata().metadata());

        writer.writeTo(withMetadata.event(), eventClass, eventType, new Annotation[0], MediaType.WILDCARD_TYPE, new MultivaluedHashMap<>(), entityStream);
    }
}
