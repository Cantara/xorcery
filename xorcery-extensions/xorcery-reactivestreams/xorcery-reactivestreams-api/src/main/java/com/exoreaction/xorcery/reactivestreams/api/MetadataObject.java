package com.exoreaction.xorcery.reactivestreams.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.metadata.WithMetadata;

public class MetadataObject<T>
        extends WithMetadata<T>
{
    public MetadataObject() {
    }

    public MetadataObject(Metadata metadata, T data) {
        super(metadata, data);
    }

    @Override
    public String toString() {
        return metadata().toString();
    }
}
