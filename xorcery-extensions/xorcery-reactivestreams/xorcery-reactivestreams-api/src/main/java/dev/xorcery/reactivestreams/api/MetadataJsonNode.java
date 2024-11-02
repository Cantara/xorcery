package dev.xorcery.reactivestreams.api;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;

public class MetadataJsonNode<T extends JsonNode>
        extends WithMetadata<T>
{
    public MetadataJsonNode() {
    }

    public MetadataJsonNode(Metadata metadata, T data) {
        super(metadata, data);
    }

    @Override
    public String toString() {
        return metadata().toString();
    }

}
