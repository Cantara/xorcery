package com.exoreaction.xorcery.reactivestreams.api.reactor;

import reactor.util.context.ContextView;

import java.util.Optional;

/**
 * Commonly used {@link ContextView} keys
 */
public enum ReactiveStreamsContext {
    streamId, // Id of stream being read or written to
    streamPosition; // Last written position when appending to a stream

    /**
     * Get value from a ContextView based on an enum key. Both the enum and enum name will be tried.
     *
     * @param contextView
     * @param key
     * @param <T>
     * @return
     */
    public static <T> Optional<T> getOptionalContext(ContextView contextView, Enum<?> key) {
        return contextView.<T>getOrEmpty(key).or(() -> contextView.getOrEmpty(key.name()));
    }

    /**
     * Get value from a ContextView based on an enum key. Both the enum and enum name will be tried.
     *
     * @param contextView
     * @param key
     * @param <T>
     * @return
     */
    public static <T> T getContext(ContextView contextView, Enum<?> key) {
        return ReactiveStreamsContext.<T>getOptionalContext(contextView, key).orElseThrow(() -> new IllegalArgumentException("Missing context:" + key.name()));
    }
}
