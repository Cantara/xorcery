package com.exoreaction.xorcery.jaxrs.writers;

import com.exoreaction.xorcery.jaxrs.MediaTypes;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Singleton
@Provider
@Produces({MediaTypes.APPLICATION_JSON_LOGEVENT, MediaType.APPLICATION_OCTET_STREAM})
public class LogEventMessageBodyWriter
    implements MessageBodyWriter<LogEvent>
{
    private final JsonTemplateLayout layout;

    public LogEventMessageBodyWriter() throws IOException {
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        String eventTemplate = new String(getClass().getResourceAsStream("/EcsLayout.json").readAllBytes(), StandardCharsets.UTF_8);
        layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(lc.getConfiguration())
                .setEventTemplate(eventTemplate)
                .build();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == LogEvent.class;
    }

    @Override
    public void writeTo(LogEvent logEvent, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {
/*
        String json = layout.toSerializable(logEvent);
        entityStream.write(json.getBytes(StandardCharsets.UTF_8));
*/
        entityStream.write(layout.toByteArray(logEvent));
    }
}
