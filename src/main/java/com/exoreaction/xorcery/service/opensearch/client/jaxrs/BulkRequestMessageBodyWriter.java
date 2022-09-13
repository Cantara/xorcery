package com.exoreaction.xorcery.service.opensearch.client.jaxrs;

import com.exoreaction.xorcery.service.opensearch.client.document.IndexBulkRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.zip.GZIPOutputStream;

@Singleton
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class BulkRequestMessageBodyWriter
        implements MessageBodyWriter<IndexBulkRequest> {
    private ObjectMapper objectMapper;

    @Inject
    public BulkRequestMessageBodyWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return IndexBulkRequest.class.equals(type);
    }

    @Override
    public void writeTo(IndexBulkRequest bulkRequest, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        if ("gzip".equals(httpHeaders.getFirst(HttpHeaders.CONTENT_ENCODING)))
        {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(entityStream);
            for (ObjectNode request : bulkRequest.requests()) {
                objectMapper.writeValue(gzipOutputStream, request);
                gzipOutputStream.write('\n');
            }
            gzipOutputStream.finish();
        } else
        {
            for (ObjectNode request : bulkRequest.requests()) {
                objectMapper.writeValue(entityStream, request);
                entityStream.write('\n');
            }
        }
    }
}
