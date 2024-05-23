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

import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

public class ByteBufferMessageWriterFactory
        implements MessageWriter.Factory {

    @Override
    public boolean canWrite(Class<?> type, String mediaType) {
        return ByteBuffer.class.isAssignableFrom(type);
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (canWrite(type, mediaType))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    static class MessageWriterImplementation
            implements MessageWriter<ByteBuffer> {
        @Override
        public void writeTo(ByteBuffer instance, OutputStream out) throws IOException {
            if (instance.hasArray())
            {
                out.write(instance.array(), instance.position(), instance.limit() - instance.position());
            } else
            {
                Channels.newChannel(out).write(instance);
            }
        }
    }
}
