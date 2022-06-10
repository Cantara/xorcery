package com.exoreaction.reactiveservices.service.handlebars.resources.writers;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.handlebars.helpers.OptionalValueResolver;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;


@Singleton
@Provider
@Produces(MediaType.TEXT_HTML)
public class HandlebarsResourceObjectMessageBodyWriter
        implements MessageBodyWriter<ResourceObject> {
    private Handlebars handlebars;
    private jakarta.inject.Provider<ContainerRequestContext> requestContext;

    @Inject
    public HandlebarsResourceObjectMessageBodyWriter(Handlebars handlebars, jakarta.inject.Provider<ContainerRequestContext> requestContext) {
        this.handlebars = handlebars;
        this.requestContext = requestContext;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ResourceObject.class;
    }

    @Override
    public void writeTo(ResourceObject resourceObject, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        Context context = Context.newBuilder(resourceObject)
                .push(OptionalValueResolver.INSTANCE)
                .build();

        handlebars.compile("jsonapi/resourceobject").apply(context, writer);

        writer.close();
    }
}
