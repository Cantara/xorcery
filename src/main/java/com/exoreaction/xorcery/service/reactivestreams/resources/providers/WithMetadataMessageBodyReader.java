package com.exoreaction.xorcery.service.reactivestreams.resources.providers;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.jaxrs.MediaTypes;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.message.MessageBodyWorkers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Singleton
@Provider
@Consumes(MediaTypes.APPLICATION_JSON_API)
public class WithMetadataMessageBodyReader
        implements MessageBodyReader<WithMetadata<Object>> {

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    private static final MultivaluedHashMap<String, String> EMPTY_HTTP_HEADERS = new MultivaluedHashMap<>();
    private static final MultivaluedHashMap<String, Object> EMPTY_HTTP_HEADERS2 = new MultivaluedHashMap<>();

    private final ObjectMapper objectMapper;
    private MessageBodyWorkers messageBodyWorkers;

    @Inject
    public WithMetadataMessageBodyReader(ObjectMapper objectMapper, MessageBodyWorkers messageBodyWorkers) {
        this.objectMapper = objectMapper;
        this.messageBodyWorkers = messageBodyWorkers;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (type.equals(WithMetadata.class)) {
            Class<?> eventType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[1];
            return !messageBodyWorkers.getMessageBodyReadersForType(eventType).isEmpty();
        }
        return false;
    }

    @Override
    public WithMetadata<Object> readFrom(Class<WithMetadata<Object>> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            JsonFactory jf = new JsonFactory(objectMapper);
            JsonParser jp = jf.createParser(entityStream);
            JsonToken metadataToken = jp.nextToken();
            Metadata metadata = jp.readValueAs(Metadata.class);

            Type eventType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
            Class<?> eventClass = (Class<?>) eventType;
            MessageBodyReader reader = messageBodyWorkers.getMessageBodyReader(eventClass, eventType, new Annotation[0], MediaType.WILDCARD_TYPE);

            Object event = reader.readFrom(eventClass, eventType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, EMPTY_HTTP_HEADERS, entityStream);
            return new WithMetadata<>(metadata, event);
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }
}
