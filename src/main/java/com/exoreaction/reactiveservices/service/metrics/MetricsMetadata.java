package com.exoreaction.reactiveservices.service.metrics;

import com.exoreaction.reactiveservices.cqrs.metadata.CommonMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

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
