package com.exoreaction.reactiveservices.service.sandbox.resources;

import com.exoreaction.reactiveservices.jetty.client.ResponseListener;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.rest.RestHelpers;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.http.HttpMethod;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

@Singleton
@Provider
@Produces(MediaType.TEXT_HTML)
public class JsonApiSandboxMessageBodyWriter
    implements MessageBodyWriter<ResourceDocument>
{
    private Handlebars handlebars;

    @Inject
    public JsonApiSandboxMessageBodyWriter(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ResourceDocument.class;
    }

    @Override
    public void writeTo(ResourceDocument resourceDocument, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        handlebars.compile("sandbox/resourcedocument").apply(Context.newContext(resourceDocument.object()), writer);
        writer.close();
    }
}
