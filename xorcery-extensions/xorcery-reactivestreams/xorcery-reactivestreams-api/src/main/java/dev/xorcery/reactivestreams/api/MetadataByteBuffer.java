package dev.xorcery.reactivestreams.api;

import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;

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
