/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.eventstore.api;

import dev.xorcery.metadata.DeploymentMetadata;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.RequestMetadata;

import java.util.Optional;

public record EventStoreMetadata(Metadata context)
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

        public Builder eventType(String value) {
            builder.add("eventType", value);
            return this;
        }

        public EventStoreMetadata build() {
            return new EventStoreMetadata(builder.build());
        }
    }

    public String streamId() {
        return context.getString("streamId").orElseThrow();
    }

    public long revision() {
        return context.getLong("revision").orElseThrow();
    }

    public Optional<Long> lastRevision() {
        return context.getLong("lastRevision");
    }

    public String eventType() {
        return context.getString("eventType").orElseThrow();
    }

    public String contentType() {
        return context.getString("contentType").orElseThrow();
    }
}
