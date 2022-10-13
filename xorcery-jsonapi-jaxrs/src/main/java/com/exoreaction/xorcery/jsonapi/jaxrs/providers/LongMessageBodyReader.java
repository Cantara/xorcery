package com.exoreaction.xorcery.jsonapi.jaxrs.providers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

@Singleton
@Provider
@Consumes("xor/long")
public class LongMessageBodyReader
        implements MessageBodyReader<Long> {

    @Inject
    public LongMessageBodyReader() {
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean result = Long.class.isAssignableFrom(type) && (mediaType.isWildcardType() || mediaType.getSubtype().endsWith("json"));
        return result;
    }

    @Override
    public Long readFrom(Class<Long> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        byte[] buf = new byte[8];
        int read = entityStream.read(buf);
        if (read != 8) {
            throw new NoContentException("Only " + read + " bytes read, should have been 8");
        }
        Long item = ByteBuffer.wrap(buf)
                .getLong();
        return item;
    }
}
