package com.exoreaction.reactiveservices.jaxrs.readers;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;
import org.apache.logging.log4j.core.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

@Singleton
@Provider
@Consumes(MediaType.APPLICATION_OCTET_STREAM)
public class LogEventMessageBodyReader
        implements MessageBodyReader<LogEvent> {

    private final JsonLogEventParser parser;

    public LogEventMessageBodyReader() {
        parser = new JsonLogEventParser();
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return LogEvent.class.equals(type);
    }

    @Override
    public LogEvent readFrom(Class<LogEvent> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(entityStream.available());
        Channels.newChannel(entityStream).read(byteBuffer);
        try {
            return parser.parseFrom(byteBuffer.array());
        } catch (ParseException e) {
            throw new IOException(e);
        }
/*
        String json = Files.read(entityStream, StandardCharsets.UTF_8);
        try {
            return parser.parseFrom(json);
        } catch (ParseException e) {
            throw new IOException(e);
        }
*/
    }
}
