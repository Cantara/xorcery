package dev.xorcery.reactivestreams.api;

import reactor.util.context.ContextView;

/**
 * Commonly used {@link ContextView} keys
 */
public enum ReactiveStreamsContext {
    streamId, // Id of stream being read or written to
    streamPosition; // Last written position when appending to a stream
}
