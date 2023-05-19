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
package com.exoreaction.xorcery.service.log4jpublisher.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;
import org.apache.logging.log4j.core.parser.ParseException;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

@Service
public class LogEventMessageReaderFactory
        implements MessageReader.Factory {

    @Override
    public <T> MessageReader<T> newReader(Class<?> type, Type genericType, String mediaType) {
        if (LogEvent.class.isAssignableFrom(type))
            return (MessageReader<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageReader<LogEvent> {

        private final JsonLogEventParser parser= new JsonLogEventParser();;

        @Override
        public LogEvent readFrom(InputStream entityStream) throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(entityStream.available());
            Channels.newChannel(entityStream).read(byteBuffer);
            try {
                return parser.parseFrom(byteBuffer.array());
            } catch (ParseException e) {
                throw new IOException(e);
            }
        }
    }
}
