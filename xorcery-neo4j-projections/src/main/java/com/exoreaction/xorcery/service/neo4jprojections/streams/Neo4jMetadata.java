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
package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;

public record Neo4jMetadata(Metadata context)
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
        return context.getLong("lastTimestamp").orElseThrow();
    }
}