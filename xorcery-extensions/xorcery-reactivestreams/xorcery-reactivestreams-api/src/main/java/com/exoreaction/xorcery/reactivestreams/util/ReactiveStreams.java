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

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ReactiveStreams {
    JsonMapper jsonMapper = new JsonMapper();

    @Deprecated
    static <T> Function<Throwable, CompletionStage<T>> cancelStream(CompletableFuture<Void> streamFuture) {
        return t ->
        {
            streamFuture.cancel(true);
            return CompletableFuture.failedStage(t);
        };
    }

    static <T> BiConsumer<T, Throwable> onErrorDispose(Disposable disposable) {
        return (v, t) ->
        {
            if (t != null) {
                disposable.dispose();
            }
        };
    }

    // Transformers
    /**
     * Converts an ObjectNode with metadata and JSON body to a MetadataByteBuffer. Use this with {@link Flux#handle(BiConsumer)}
     */
    static BiConsumer<ObjectNode, SynchronousSink<MetadataByteBuffer>> toMetadataByteBuffer(String metadataProperty, String jsonProperty) {
        return (json, sink) ->
        {
            try {
                Metadata metadata = new Metadata(Optional.ofNullable((ObjectNode) json.get(metadataProperty)).orElseGet(JsonNodeFactory.instance::objectNode));
                ByteBuffer byteBuffer = ByteBuffer.wrap(jsonMapper.writeValueAsBytes(Optional.ofNullable(json.get(jsonProperty)).orElseGet(JsonNodeFactory.instance::objectNode)));
                sink.next(new MetadataByteBuffer(metadata, byteBuffer));
            } catch (JsonProcessingException e) {
                sink.error(e);
            }
        };
    }
}
