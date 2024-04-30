package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.neo4jprojections.api.ProjectionCommit;

public class MetadataProjectionCommit
    extends com.exoreaction.xorcery.metadata.WithMetadata<ProjectionCommit>
{
    public MetadataProjectionCommit() {
    }

    public MetadataProjectionCommit(Metadata metadata, ProjectionCommit data) {
        super(metadata, data);
    }

    @Override
    public String toString() {
        return metadata().toString();
    }
}
