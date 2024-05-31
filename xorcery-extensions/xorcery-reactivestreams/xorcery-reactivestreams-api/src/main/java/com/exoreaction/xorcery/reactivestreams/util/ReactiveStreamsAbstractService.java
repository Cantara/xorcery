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

import com.exoreaction.xorcery.lang.Classes;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Deprecated
public abstract class ReactiveStreamsAbstractService {
    // Magic bytes for sending exceptions
    public static final byte[] XOR = "XOR".getBytes(StandardCharsets.UTF_8);

    protected final MessageWorkers messageWorkers;
    protected final ObjectMapper objectMapper;

    public ReactiveStreamsAbstractService(MessageWorkers messageWorkers) {
        this.messageWorkers = messageWorkers;
        this.objectMapper = new ObjectMapper();
    }

    protected Type getEventType(Type type) {
        return type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[0] : type;
    }

    protected Optional<Type> getResultType(Type type) {
        return Optional.ofNullable(type instanceof ParameterizedType pt && pt.getRawType().equals(WithResult.class) ? pt.getActualTypeArguments()[1] : null);
    }

    protected MessageWriter<Object> getWriter(Type type) {
        return Optional.ofNullable(messageWorkers.newWriter(Classes.getClass(type), type, null))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + type));
    }

    protected MessageReader<Object> getReader(Type type) {
        return Optional.ofNullable(messageWorkers.newReader(Classes.getClass(type), type, null))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageReader for " + type));
    }
}
