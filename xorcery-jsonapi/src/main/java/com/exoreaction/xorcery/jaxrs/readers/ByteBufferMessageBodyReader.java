package com.exoreaction.xorcery.jaxrs.readers;

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
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

@Singleton
@Provider
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
public class ByteBufferMessageBodyReader
        implements MessageBodyReader<ByteBuffer> {

    @Inject
    public ByteBufferMessageBodyReader() {
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ByteBuffer.class.isAssignableFrom(type);
    }

    @Override
    public ByteBuffer readFrom(Class<ByteBuffer> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {

        ByteBuffer byteBuffer = ByteBuffer.allocate(entityStream.available());
        Channels.newChannel(entityStream).read(byteBuffer);
        return byteBuffer;
    }
}
