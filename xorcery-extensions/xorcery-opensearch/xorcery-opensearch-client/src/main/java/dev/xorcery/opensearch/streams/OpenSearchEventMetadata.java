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
package dev.xorcery.opensearch.streams;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.metadata.CommonMetadata;
import dev.xorcery.metadata.DeploymentMetadata;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.RequestMetadata;

public record OpenSearchEventMetadata(Metadata context)
        implements CommonMetadata, RequestMetadata, DeploymentMetadata
{
    public OpenSearchEventMetadata(ObjectNode metadata) {
        this(new Metadata(metadata));
    }

    public record Builder(Metadata.Builder builder)
            implements CommonMetadata.Builder<Builder>,
            RequestMetadata.Builder<Builder>,
            DeploymentMetadata.Builder<Builder>
    {
        public Builder(Metadata metadata) {
            this(metadata.toBuilder());
        }

        public OpenSearchEventMetadata build()
        {
            return new OpenSearchEventMetadata(builder.build());
        }
    }
}
