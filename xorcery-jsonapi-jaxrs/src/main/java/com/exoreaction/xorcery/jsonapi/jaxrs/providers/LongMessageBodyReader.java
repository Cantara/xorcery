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
