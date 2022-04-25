package com.exoreaction.reactiveservices.jaxrs.writer;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Provider
@Produces(MediaType.TEXT_HTML)
public class HandlebarsHtmlMessageBodyWriter
    implements MessageBodyWriter<Context>
{

    private Handlebars handlebars;

    jakarta.inject.Provider<ContainerRequestContext> requestContextProvider;

    @Inject
    public HandlebarsHtmlMessageBodyWriter(Handlebars handlebars,
                                           jakarta.inject.Provider<ContainerRequestContext> requestContextProvider)
    {
        this.handlebars = handlebars;
        this.requestContextProvider = requestContextProvider;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == Context.class;
    }

    @Override
    public void writeTo(Context context, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        String path = requestContextProvider.get().getUriInfo().getPath();
        Template compile = handlebars.compile(path);
        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        compile.apply(context, writer);
        writer.close();
    }
}
