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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MessageWorkers {

    private final Iterable<MessageWriter.Factory> writers;
    private final Iterable<MessageReader.Factory> readers;

    public MessageWorkers(Iterable<MessageWriter.Factory> writers, Iterable<MessageReader.Factory> readers) {
        this.writers = StreamSupport.stream(writers.spliterator(), false).collect(Collectors.toList());
        this.readers = StreamSupport.stream(readers.spliterator(), false).collect(Collectors.toList());
    }

    public boolean canRead(Class<?> type, String contentType) {
        if (contentType.equals("*/*")) {
            for (MessageReader.Factory factory : readers) {
                if (factory.canRead(type, factory.getContentType(type)))
                    return true;
            }
        } else {
            for (MessageReader.Factory factory : readers) {
                if (factory.canRead(type, contentType))
                    return true;
            }
        }
        return false;
    }

    public boolean canWrite(Class<?> type, String contentType) {
        if (contentType.equals("*/*")) {
            for (MessageWriter.Factory factory : writers) {
                if (factory.canWrite(type, factory.getContentType(type)))
                    return true;
            }
        } else {
            for (MessageWriter.Factory factory : writers) {
                if (factory.canWrite(type, contentType))
                    return true;
            }
        }
        return false;
    }

    public Collection<String> getAvailableReadContentTypes(Class<?> type, Collection<String> acceptableContentTypes) {
        List<String> contentTypes = new ArrayList<>();
        if (acceptableContentTypes.isEmpty()) {
            for (MessageReader.Factory reader : readers) {
                String contentType = reader.getContentType(type);
                if (!contentTypes.contains(contentType) && reader.canRead(type, contentType)) {
                    contentTypes.add(contentType);
                }
            }
        } else {
            for (String acceptableContentType : acceptableContentTypes) {
                for (MessageReader.Factory reader : readers) {
                    if (!contentTypes.contains(acceptableContentType) && reader.canRead(type, acceptableContentType)) {
                        contentTypes.add(acceptableContentType);
                        break;
                    }
                }
            }
        }
        return contentTypes;
    }

    public Collection<String> getAvailableWriteContentTypes(Class<?> type, Collection<String> acceptableContentTypes) {
        List<String> contentTypes = new ArrayList<>();
        if (acceptableContentTypes.isEmpty()) {
            for (MessageWriter.Factory writer : writers) {
                String contentType = writer.getContentType(type);
                if (!contentTypes.contains(contentType) && writer.canWrite(type, contentType)) {
                    contentTypes.add(contentType);
                }
            }
        } else {
            for (String acceptableContentType : acceptableContentTypes) {
                for (MessageWriter.Factory writer : writers) {
                    if (!contentTypes.contains(acceptableContentType) && writer.canWrite(type, acceptableContentType)) {
                        contentTypes.add(acceptableContentType);
                        break;
                    }
                }
            }
        }
        return contentTypes;
    }

    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String contentType) {
        MessageWriter<T> candidate = null;
        if (contentType == null) {
            for (MessageWriter.Factory factory : writers) {
                MessageWriter<T> writer = factory.newWriter(type, genericType, factory.getContentType(type));
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

        } else {
            for (MessageWriter.Factory factory : writers) {
                MessageWriter<T> writer = factory.newWriter(type, genericType, contentType);
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
        }

        if (candidate != null)
            return candidate;
        else
            throw new IllegalArgumentException("No MessageWriter found for type:" + type + ", content type:" + contentType);
    }

    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String contentType) {
        MessageReader<T> candidate = null;
        if (contentType == null) {
            for (MessageReader.Factory factory : readers) {
                MessageReader<T> reader = factory.newReader(type, genericType, factory.getContentType(type));
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

        } else {
            for (MessageReader.Factory factory : readers) {
                MessageReader<T> reader = factory.newReader(type, genericType, contentType);
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
        }

        if (candidate != null)
            return candidate;
        else
            throw new IllegalArgumentException("No MessageReader found for type:" + type + ", content type:" + contentType);
    }

    private Class<?> getRootType(Type type) {
        if (type instanceof ParameterizedType)
            return (Class<?>) ((ParameterizedType) type).getOwnerType();
        else
            return (Class<?>) type;
    }
}
