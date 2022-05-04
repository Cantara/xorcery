package com.exoreaction.reactiveservices.jaxrs.writers;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Singleton
@Provider
@Produces(MediaTypes.APPLICATION_JSON_LOGEVENT)
public class LogEventMessageBodyWriter
    implements MessageBodyWriter<LogEvent>
{
    private final JsonLayout layout;

    public LogEventMessageBodyWriter() {
        layout = JsonLayout.createDefaultLayout();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == LogEvent.class;
    }

    @Override
    public void writeTo(LogEvent logEvent, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
        OutputStreamWriter writer = new OutputStreamWriter(entityStream);
        layout.toSerializable(logEvent, writer);
        writer.close();
    }
}
