package com.exoreaction.xorcery.service.opensearch.streams;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.metadata.RequestMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record OpenSearchEventMetadata(Metadata context)
        implements CommonMetadata, RequestMetadata, DeploymentMetadata
{
    public OpenSearchEventMetadata(ObjectNode metadata) {
        this(new Metadata(metadata));
    }

    public record Builder(Metadata.Builder builder)
            implements CommonMetadata.Builder<Builder>,
            RequestMetadata.Builder<Builder>,
            DeploymentMetadata.Builder<Builder>
    {
        public Builder(Metadata metadata) {
            this(metadata.toBuilder());
        }

        public OpenSearchEventMetadata build()
        {
            return new OpenSearchEventMetadata(builder.build());
        }
    }
}
