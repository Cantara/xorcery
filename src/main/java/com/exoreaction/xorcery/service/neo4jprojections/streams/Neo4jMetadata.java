package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;

public record Neo4jMetadata(Metadata metadata)
        implements CommonMetadata, DeploymentMetadata {

    public record Builder(Metadata.Builder builder)
            implements CommonMetadata.Builder<Neo4jMetadata.Builder>, DeploymentMetadata.Builder<Neo4jMetadata.Builder> {

        Builder lastTimestamp(long value)
        {
            builder.add("lastTimestamp", value);
            return this;
        }

        public Neo4jMetadata build() {
            return new Neo4jMetadata(builder.build());
        }
    }

    public long lastTimestamp()
    {
        return metadata.getLong("lastTimestamp").orElseThrow();
    }
}