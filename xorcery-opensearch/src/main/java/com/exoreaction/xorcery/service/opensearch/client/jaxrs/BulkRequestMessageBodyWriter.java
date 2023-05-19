/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.service.opensearch.client.jaxrs;

import com.exoreaction.xorcery.service.opensearch.client.document.IndexBulkRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.zip.GZIPOutputStream;

@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class BulkRequestMessageBodyWriter
        implements MessageBodyWriter<IndexBulkRequest> {
    private ObjectMapper objectMapper;

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
