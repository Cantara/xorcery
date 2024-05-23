package com.exoreaction.xorcery.reactivestreams.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.metadata.WithMetadata;

import java.nio.ByteBuffer;

public class MetadataByteBuffer
        extends WithMetadata<ByteBuffer>
{
    public MetadataByteBuffer() {
    }

    public MetadataByteBuffer(Metadata metadata, ByteBuffer data) {
        super(metadata, data);
    }

    @Override
    public String toString() {
        return metadata().toString();
    }
}
