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
package dev.xorcery.reactivestreams.providers;

import dev.xorcery.reactivestreams.spi.MessageWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class ByteArrayMessageWriterFactory
        implements MessageWriter.Factory {

    @Override
    public boolean canWrite(Class<?> type, String mediaType) {
        return byte[].class.isAssignableFrom(type);
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (canWrite(type, mediaType))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    static class MessageWriterImplementation
            implements MessageWriter<byte[]> {
        @Override
        public void writeTo(byte[] instance, OutputStream out) throws IOException {
            out.write(instance);
        }
    }
}
