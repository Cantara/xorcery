package com.exoreaction.xorcery.eventstore.client.api;

/**
 * Used as keys with {@link reactor.util.context.ContextView} when appending to streams
 */
public enum EventStoreContext {
    maxAge,
    maxCount,
    cacheControl,
    truncateBefore,
    acl,
    customProperties,

    // For readStream, if true stream will not end when there are no more events
    keepAlive
}
