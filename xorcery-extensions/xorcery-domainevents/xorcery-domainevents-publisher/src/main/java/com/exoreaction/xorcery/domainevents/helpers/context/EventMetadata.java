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
package com.exoreaction.xorcery.domainevents.helpers.context;

import com.exoreaction.xorcery.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.domainevents.helpers.entity.Command;
import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.metadata.RequestMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.exoreaction.xorcery.metadata.Metadata.missing;

public record EventMetadata(Metadata context)
        implements CommonMetadata, RequestMetadata, DeploymentMetadata {
    public EventMetadata(ObjectNode metadata) {
        this(new Metadata(metadata));
    }

    public record Builder(Metadata.Builder builder)
            implements CommonMetadata.Builder<Builder>,
            RequestMetadata.Builder<Builder>,
            DeploymentMetadata.Builder<Builder> {
        public static EventMetadata aggregateId(String aggregateId, Metadata metadata) {
            return new Builder(metadata).aggregateId(aggregateId).build();
        }

        public static EventMetadata aggregateType(String aggregateType, Metadata metadata) {
            return new Builder(metadata).aggregateType(aggregateType).build();
        }

        public static EventMetadata aggregate(String aggregateType, String aggregateId, Metadata metadata) {
            return new Builder(metadata)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .build();
        }

        public Builder(Metadata metadata) {
            this(metadata.toBuilder());
        }

        public Builder domain(String value) {
            builder.add(DomainEventMetadata.domain, value);
            return this;
        }

        public Builder tenantId(String value) {
            builder.add(DomainEventMetadata.tenantId, value);
            return this;
        }

        public Builder aggregateId(String value) {
            builder.add(DomainEventMetadata.aggregateId, value);
            return this;
        }

        public Builder aggregateType(String name) {
            builder.add(DomainEventMetadata.aggregateType, name);
            return this;
        }

        public Builder commandName(String commandName) {
            builder.add(DomainEventMetadata.commandName, commandName);
            return this;
        }

        public Builder commandName(Class<? extends Command> commandClass) {
            builder.add(DomainEventMetadata.commandName, commandClass.getSimpleName());
            return this;
        }

        public EventMetadata build() {
            return new EventMetadata(builder.build());
        }
    }

    public String getTenantId() {
        return context.getString(DomainEventMetadata.tenantId.name()).orElseThrow(missing(DomainEventMetadata.tenantId));
    }

    public String getDomain() {
        return context.getString(DomainEventMetadata.domain.name()).orElse("default");
    }

    public String getAggregateType() {
        return context.getString(DomainEventMetadata.aggregateType.name()).orElseThrow(missing(DomainEventMetadata.aggregateType));
    }

    public String getAggregateId() {
        return context.getString(DomainEventMetadata.aggregateId.name()).orElseThrow(missing(DomainEventMetadata.aggregateId));
    }

    public String getCommandName() {
        return context.getString(DomainEventMetadata.commandName.name()).orElseThrow(missing(DomainEventMetadata.commandName));
    }
}
