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
package com.exoreaction.xorcery.reactivestreams.providers;

import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public class ByteBufferMessageReaderFactory
        implements MessageReader.Factory {

    @Override
    public boolean canRead(Class<?> type, String mediaType) {
        return ByteBuffer.class.isAssignableFrom(type);
    }

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (ByteBuffer.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageReaderImplementation();
        else
            return null;
    }

    static class MessageReaderImplementation
            implements MessageReader<ByteBuffer> {

        @Override
        public ByteBuffer readFrom(InputStream entityStream) throws IOException {
            return ByteBuffer.wrap(entityStream.readAllBytes());
        }
    }
}
