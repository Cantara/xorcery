package com.exoreaction.xorcery.reactivestreams.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.databind.JsonNode;

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
