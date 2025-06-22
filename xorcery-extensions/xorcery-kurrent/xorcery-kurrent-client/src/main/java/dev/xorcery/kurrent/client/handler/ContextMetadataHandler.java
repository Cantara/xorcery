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
package dev.xorcery.kurrent.client.handler;

import dev.xorcery.metadata.Metadata;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ContextMetadataHandler
        implements BiFunction<Flux<MetadataByteBuffer>, ContextView, Publisher<MetadataByteBuffer>> {
    @Override
    public Publisher<MetadataByteBuffer> apply(Flux<MetadataByteBuffer> metadataByteBufferFlux, ContextView contextView) {
        return metadataByteBufferFlux.map(addContextAsMetadata(contextView));
    }

    private Function<? super MetadataByteBuffer, ? extends MetadataByteBuffer> addContextAsMetadata(ContextView contextView) {
        return metadataByteBuffer ->
        {
            Metadata.Builder builder = metadataByteBuffer.metadata().toBuilder();
            contextView.forEach((k, v) -> {
                // TODO Handle more types of values?
                if (v instanceof String str) {
                    builder.add(k.toString(), str);
                } else if (v instanceof Long nr) {
                    builder.add(k.toString(), nr);
                }
            });
            return metadataByteBuffer;
        };
    }
}
