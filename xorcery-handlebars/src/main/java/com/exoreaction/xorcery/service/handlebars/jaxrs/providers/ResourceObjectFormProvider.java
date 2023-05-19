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
package com.exoreaction.xorcery.service.handlebars.jaxrs.providers;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.util.Strings;
import org.glassfish.jersey.internal.util.collection.NullableMultivaluedHashMap;
import org.glassfish.jersey.message.internal.AbstractFormProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Produces({"application/x-www-form-urlencoded", "*/*"})
@Consumes({"application/x-www-form-urlencoded"})
@Singleton
@Provider
public final class ResourceObjectFormProvider extends AbstractFormProvider<ResourceObject> {

    jakarta.inject.Provider<ContainerRequestContext> requestContextProvider;

    @Inject
    public ResourceObjectFormProvider(jakarta.inject.Provider<ContainerRequestContext> requestContextProvider)
    {
        this.requestContextProvider = requestContextProvider;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ResourceObject.class;
    }

    @Override
    public ResourceObject readFrom(
            Class<ResourceObject> type,
            Type genericType,
            Annotation annotations[],
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException {

        NullableMultivaluedHashMap<String, String> map = this.readFrom(new NullableMultivaluedHashMap<>(), mediaType, decode(annotations), entityStream);

        String id = Strings.trimToNull(map.getFirst("id"));
        return new ResourceObject.Builder(requestContextProvider.get().getUriInfo().getQueryParameters().getFirst("rel"), id).attributes(new Attributes.Builder().with(builder ->
        {
            map.forEach((name, value) ->
            {
                builder.attribute(name, value.get(0));
            });
        })).build();
    }


    private boolean decode(Annotation annotations[]) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(Encoded.class)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Form.class;
    }

    @Override
    public void writeTo(
            ResourceObject ro,
            Class<?> type,
            Type genericType,
            Annotation annotations[],
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        writeTo(new MultivaluedHashMap<>(ro.getAttributes().toMap()), mediaType, entityStream);
    }
}
