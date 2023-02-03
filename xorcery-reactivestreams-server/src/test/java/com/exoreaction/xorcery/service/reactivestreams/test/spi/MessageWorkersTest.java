package com.exoreaction.xorcery.service.reactivestreams.test.spi;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

public class MessageWorkersTest {

    @Test
    public void givenMultipleWritersWhenNewWriterThenPickMostSpecificWriter() {

        {
            // Given
            MessageWorkers workers = new MessageWorkers(List.of(new ObjectMessageWriterFactory(), new StringMessageWriterFactory()), List.of());
            // When
            MessageWriter writer = workers.newWriter(String.class, String.class, "text/plain");
            // Then
            assertThat(writer, instanceOf(StringMessageWriterFactory.StringMessageWriter.class));
        }

        {
            // Given
            MessageWorkers workers = new MessageWorkers(List.of(new ObjectMessageWriterFactory(), new StringMessageWriterFactory()), List.of());
            // When
            MessageWriter writer = workers.newWriter(Object.class, Object.class, "application/octet-stream");
            // Then
            assertThat(writer, instanceOf(ObjectMessageWriterFactory.ObjectMessageWriter.class));
        }

    }

    public class ObjectMessageWriterFactory
            implements MessageWriter.Factory {
        @Override
        public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
            return (MessageWriter<T>) new ObjectMessageWriter();
        }

        static class ObjectMessageWriter
                implements MessageWriter<Object> {
            @Override
            public void writeTo(Object instance, OutputStream out) throws IOException {
                out.write(instance.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public class StringMessageWriterFactory
            implements MessageWriter.Factory {
        @Override
        public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
            if (type.equals(String.class))
                return (MessageWriter<T>) new StringMessageWriter();
            else
                return null;
        }

        static class StringMessageWriter
                implements MessageWriter<String> {
            @Override
            public void writeTo(String instance, OutputStream out) throws IOException {
                out.write(instance.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
