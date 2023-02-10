package com.exoreaction.xorcery.service.log4jpublisher.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Service
public class LogEventMessageWriterFactory
        implements MessageWriter.Factory {

    private final JsonTemplateLayout layout;

    public LogEventMessageWriterFactory() throws IOException {
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        String eventTemplate = new String(JsonTemplateLayout.class.getResourceAsStream("/JsonLayout.json").readAllBytes(), StandardCharsets.UTF_8);
        layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(lc.getConfiguration())
                .setEventTemplate(eventTemplate)
                .build();
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (LogEvent.class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<LogEvent> {
        @Override
        public void writeTo(LogEvent instance, OutputStream out) throws IOException {
            out.write(layout.toByteArray(instance));
        }
    }
}