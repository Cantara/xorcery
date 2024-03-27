package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
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
                if (me.getMetadata().getLong(contextKey)
                        .map(revision -> revision > projectionRevision).orElse(true))
                    sink.next(me);
            });
        } else
        {
            return metadataEventsFlux;
        }
    }
}
