package com.exoreaction.xorcery.service.reactivestreams.spi;

import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Service
public class MessageWorkers {

    private final Iterable<MessageWriter.Factory> writers;
    private final Iterable<MessageReader.Factory> readers;

    @Inject
    public MessageWorkers(Iterable<MessageWriter.Factory> writers, Iterable<MessageReader.Factory> readers) {
        this.writers = writers;
        this.readers = readers;
    }

    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        MessageWriter<T> candidate = null;
        for (MessageWriter.Factory factory : writers) {
            MessageWriter<T> writer = factory.newWriter(type, genericType, mediaType);
            if (writer != null) {
                if (candidate == null) {
                    candidate = writer;
                } else {
                    Class<?> candidateMessageType = getRootType(((ParameterizedType) candidate.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
                    Class<?> writerMessageType = getRootType(((ParameterizedType) writer.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
                    if (candidateMessageType.isAssignableFrom(writerMessageType))
                        candidate = writer;
                }
            }
        }

        if (candidate != null)
            return candidate;
        else
            throw new IllegalArgumentException("No MessageWriter found for type:" + type + ", mediatype:" + mediaType);
    }

    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        MessageReader<T> candidate = null;
        for (MessageReader.Factory factory : readers) {
            MessageReader<T> reader = factory.newReader(type, genericType, mediaType);
            if (reader != null) {
                if (candidate == null) {
                    candidate = reader;
                } else {
                    Class<?> candidateMessageType = getRootType(((ParameterizedType) candidate.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
                    Class<?> writerMessageType = getRootType(((ParameterizedType) reader.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0]);
                    if (candidateMessageType.isAssignableFrom(writerMessageType))
                        candidate = reader;
                }
            }
        }

        if (candidate != null)
            return candidate;
        else
            throw new IllegalArgumentException("No MessageReader found for type:" + type + ", mediatype:" + mediaType);
    }

    private Class<?> getRootType(Type type) {
        if (type instanceof ParameterizedType)
            return (Class<?>) ((ParameterizedType) type).getOwnerType();
        else
            return (Class<?>) type;
    }
}
