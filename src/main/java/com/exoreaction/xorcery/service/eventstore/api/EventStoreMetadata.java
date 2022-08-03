package com.exoreaction.xorcery.service.eventstore.api;

import com.exoreaction.xorcery.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.cqrs.metadata.RequestMetadata;

import java.util.Optional;

public record EventStoreMetadata(Metadata metadata)
    implements RequestMetadata, DeploymentMetadata
{

    public record Builder(Metadata.Builder builder)
        implements RequestMetadata.Builder<Builder>, DeploymentMetadata.Builder<Builder>
    {
        public Builder streamId(String value) {
            builder.add("streamId", value);
            return this;
        }

        public Builder revision(long value) {
            builder.add("revision", value);
            return this;
        }

        public Builder lastRevision(long value) {
            builder.add("lastRevision", value);
            return this;
        }

        public Builder contentType(String value) {
            builder.add("contentType", value);
            return this;
        }

        public EventStoreMetadata build() {
            return new EventStoreMetadata(builder.build());
        }
    }

    public long revision() {
        return metadata.getLong("revision").orElseThrow();
    }

    public Optional<Long> lastRevision() {
        return metadata.getLong("lastRevision");
    }

    public String contentType() {
        return metadata.getString("contentType").orElseThrow();
    }
}
