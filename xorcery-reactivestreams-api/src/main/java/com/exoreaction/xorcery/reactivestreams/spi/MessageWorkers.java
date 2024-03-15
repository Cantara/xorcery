/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MessageWorkers {

    private final Iterable<MessageWriter.Factory> writers;
    private final Iterable<MessageReader.Factory> readers;

    public MessageWorkers(Iterable<MessageWriter.Factory> writers, Iterable<MessageReader.Factory> readers) {
        this.writers = StreamSupport.stream(writers.spliterator(), false).collect(Collectors.toList());
        this.readers = StreamSupport.stream(readers.spliterator(), false).collect(Collectors.toList());
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
