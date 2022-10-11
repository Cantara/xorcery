package com.exoreaction.xorcery.service.opensearch.streams;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;

public record OpenSearchMetadata(Metadata context)
        implements CommonMetadata, DeploymentMetadata {

    public record Builder(Metadata.Builder builder)
            implements CommonMetadata.Builder<OpenSearchMetadata.Builder>, DeploymentMetadata.Builder<OpenSearchMetadata.Builder> {

        public OpenSearchMetadata build() {
            return new OpenSearchMetadata(builder.build());
        }
    }
}
