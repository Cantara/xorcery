package com.exoreaction.xorcery.service.metrics;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;

public record MetricsMetadata(Metadata metadata)
    implements CommonMetadata, DeploymentMetadata
{
    public record Builder(Metadata.Builder builder)
        implements CommonMetadata.Builder<Builder>, DeploymentMetadata.Builder<Builder>
    {
        public MetricsMetadata build() {
            return new MetricsMetadata(builder.build());
        }
    }
}
