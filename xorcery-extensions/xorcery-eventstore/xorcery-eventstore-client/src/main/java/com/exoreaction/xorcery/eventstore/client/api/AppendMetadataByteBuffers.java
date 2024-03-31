package com.exoreaction.xorcery.eventstore.client.api;

import java.util.Collection;

/**
 * The structure used to append data into a stream
 *
 * @param streamId
 * @param expectedPosition may be null
 * @param items
 */
public record AppendMetadataByteBuffers(String streamId, Long expectedPosition, Collection<MetadataByteBuffer> items) {
}
