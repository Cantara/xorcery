package com.exoreaction.xorcery.eventstore.reactor;

import java.util.List;

public record AppendMetadataByteBuffers(String stream, List<MetadataByteBuffer> items) {
}
