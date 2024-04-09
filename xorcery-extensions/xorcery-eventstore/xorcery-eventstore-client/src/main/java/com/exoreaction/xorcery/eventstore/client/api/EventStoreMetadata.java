package com.exoreaction.xorcery.eventstore.client.api;

/**
 * Metadata keys used by EventStoreClient
 */
public enum EventStoreMetadata {
    streamId, // String id of stream being read from
    streamPosition, // Long position of event in stream
    streamLive, // Boolean of whether the stream is now live (true on "caughtup" and false on "fellbehind")
    originalStreamId, // String id of stream an event was originally written to
    expectedPosition // Expected position of stream to write to (used for optimistic locking)
}
