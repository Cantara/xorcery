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
package com.exoreaction.xorcery.service.reactivestreams.test.media;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

@Service
public class LongMessageWriterFactory
        implements MessageWriter.Factory {

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (Long.class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<Long> {
        @Override
        public void writeTo(Long instance, OutputStream out) throws IOException {
            byte[] buf = new byte[8];
            ByteBuffer.wrap(buf).putLong(instance);
            out.write(buf);
        }
    }
}
