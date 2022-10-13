package com.exoreaction.xorcery.jsonapi.jaxrs.providers;

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
import java.nio.ByteBuffer;

@Singleton
@Provider
@Produces("xor/long")
public class LongMessageBodyWriter
        implements MessageBodyWriter<Long> {

    @Inject
    public LongMessageBodyWriter() {
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean result = Long.class.isAssignableFrom(type) && (mediaType.isWildcardType() || mediaType.getSubtype().endsWith("json"));
        return result;
    }

    @Override
    public void writeTo(Long item, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        byte[] buf = new byte[8];
        ByteBuffer.wrap(buf)
                .putLong(item);
        entityStream.write(buf);
    }
}
