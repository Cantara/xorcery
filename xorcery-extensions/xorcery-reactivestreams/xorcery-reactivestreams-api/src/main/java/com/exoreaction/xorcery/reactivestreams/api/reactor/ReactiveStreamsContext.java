package com.exoreaction.xorcery.reactivestreams.api.reactor;

import reactor.util.context.ContextView;

import java.util.Optional;

/**
 * Commonly used {@link ContextView} keys
 */
public enum ReactiveStreamsContext {
    streamId, // Id of stream being read or written to
    streamPosition; // Last written position when appending to a stream
}
