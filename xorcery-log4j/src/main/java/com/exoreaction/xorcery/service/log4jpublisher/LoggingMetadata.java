package com.exoreaction.xorcery.service.log4jpublisher;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;

public record LoggingMetadata(Metadata context)
    implements CommonMetadata, DeploymentMetadata
{
    public record Builder(Metadata.Builder builder)
        implements CommonMetadata.Builder<Builder>, DeploymentMetadata.Builder<Builder>
    {
        public LoggingMetadata build() {
            return new LoggingMetadata(builder.build());
        }
    }
}
