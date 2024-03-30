package com.exoreaction.xorcery.eventstore.client;

import java.util.List;

public record AppendMetadataByteBuffers(String stream, List<MetadataByteBuffer> items) {
}
