package com.exoreaction.xorcery.eventstore.client.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;

import java.nio.ByteBuffer;

public class MetadataByteBuffer
    extends WithMetadata<ByteBuffer>
{
    public MetadataByteBuffer() {
    }

    public MetadataByteBuffer(Metadata metadata, ByteBuffer event) {
        super(metadata, event);
    }

    @Override
    public String toString() {
        return metadata().toString();
    }
}
