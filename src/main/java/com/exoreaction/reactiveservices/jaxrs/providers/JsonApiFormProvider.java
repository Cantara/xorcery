package com.exoreaction.reactiveservices.jaxrs.providers;

import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.internal.util.collection.NullableMultivaluedHashMap;
import org.glassfish.jersey.message.internal.AbstractFormProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Produces({"application/x-www-form-urlencoded", "*/*"})
@Consumes({"application/x-www-form-urlencoded", "*/*"})
@Singleton
@Provider
public final class JsonApiFormProvider extends AbstractFormProvider<ResourceDocument> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ResourceDocument.class;
    }

    @Override
    public ResourceDocument readFrom(
            Class<ResourceDocument> type,
            Type genericType,
            Annotation annotations[],
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream entityStream) throws IOException {

        NullableMultivaluedHashMap<String, String> map = this.readFrom(new NullableMultivaluedHashMap<>(), mediaType, decode(annotations), entityStream);

        return new ResourceDocument.Builder().data(new ResourceObject.Builder("form").attributes(new Attributes.Builder().with(builder ->
        {
            map.forEach((name, value) ->
            {
                builder.attribute(name, value.get(0));
            });
        }))).build();
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
            ResourceDocument t,
            Class<?> type,
            Type genericType,
            Annotation annotations[],
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        writeTo(new MultivaluedHashMap<>(t.getResource().map(ro -> ro.getAttributes().toMap()).orElseGet(Collections::emptyMap)), mediaType, entityStream);
    }
}
