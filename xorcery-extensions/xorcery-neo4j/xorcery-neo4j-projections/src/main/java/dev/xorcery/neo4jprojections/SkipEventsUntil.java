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
package dev.xorcery.neo4jprojections;

import dev.xorcery.domainevents.api.MetadataEvents;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

public class SkipEventsUntil
        implements BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>>
{
    private final String contextKey;

    public SkipEventsUntil(String contextKey) {
        this.contextKey = contextKey;
    }

    @Override
    public Publisher<MetadataEvents> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {
        if (contextView.hasKey(contextKey))
        {
            long projectionRevision = contextView.get(contextKey);
            return metadataEventsFlux.handle((me, sink)->
            {
                if (me.metadata().getLong(contextKey)
                        .map(revision -> revision > projectionRevision).orElse(true))
                    sink.next(me);
            });
        } else
        {
            return metadataEventsFlux;
        }
    }
}
