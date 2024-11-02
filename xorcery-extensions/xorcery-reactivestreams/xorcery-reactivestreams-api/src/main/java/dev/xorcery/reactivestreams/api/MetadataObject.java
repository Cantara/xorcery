package dev.xorcery.reactivestreams.api;

import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;

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
