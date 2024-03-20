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
package com.exoreaction.xorcery.reactivestreams.util;

import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.SynchronousSink;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Flux.handle() consumer that converts items from one type to another using specified MessageWriter/MessageReader.
 *
 * @param <INPUT>
 * @param <OUTPUT>
 */
public class TypeConverter<INPUT, OUTPUT>
        implements BiConsumer<INPUT, SynchronousSink<OUTPUT>> {

    private final MessageWriter<INPUT> writer;
    private final MessageReader<OUTPUT> reader;

    public TypeConverter(MessageWriter<INPUT> writer, MessageReader<OUTPUT> reader) {
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    public void accept(INPUT input, SynchronousSink<OUTPUT> sink) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            writer.writeTo(input, bout);
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            OUTPUT newItem = reader.readFrom(bin);
            sink.next(newItem);
        } catch (IOException e) {
            sink.error(e);
        }
    }
}
