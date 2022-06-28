package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.cqrs.metadata.CommonMetadata;
import com.exoreaction.xorcery.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;

public record LoggingMetadata(Metadata metadata)
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
