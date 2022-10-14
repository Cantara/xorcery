package com.exoreaction.xorcery.jsonapi.jaxrs.providers;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

@Singleton
@Provider
@Consumes(MediaType.WILDCARD)
public class ByteBufferMessageBodyWriter
        implements MessageBodyWriter<ByteBuffer> {

    public ByteBufferMessageBodyWriter() {
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ByteBuffer.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(ByteBuffer byteBuffer, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(byteBuffer.array());
    }
}
