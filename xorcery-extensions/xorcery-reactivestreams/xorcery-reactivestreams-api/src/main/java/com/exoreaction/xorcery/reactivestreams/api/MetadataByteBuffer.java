package com.exoreaction.xorcery.reactivestreams.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.ByteBuffer;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;

public class MetadataByteBuffer
        extends WithMetadata<ByteBuffer>
{
    public MetadataByteBuffer() {
    }

    @JsonCreator(mode = PROPERTIES)
    public MetadataByteBuffer(@JsonProperty("metadata") Metadata metadata, @JsonProperty("data") ByteBuffer data) {
        super(metadata, data);
    }

    @Override
    public String toString() {
        return metadata().toString();
    }
}
