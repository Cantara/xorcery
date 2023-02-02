package com.exoreaction.xorcery.domainevents.helpers.context;

import com.exoreaction.xorcery.domainevents.helpers.entity.Command;
import com.exoreaction.xorcery.metadata.CommonMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.metadata.RequestMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record DomainEventMetadata(Metadata context)
    implements CommonMetadata, RequestMetadata, DeploymentMetadata
{
    public DomainEventMetadata(ObjectNode metadata) {
        this(new Metadata(metadata));
    }

    public record Builder(Metadata.Builder builder)
        implements CommonMetadata.Builder<Builder>,
            RequestMetadata.Builder<Builder>,
            DeploymentMetadata.Builder<Builder>
    {
        public static DomainEventMetadata aggregateId(String aggregateId, Metadata metadata)
        {
            return new Builder(metadata).aggregateId(aggregateId).build();
        }

        public static DomainEventMetadata aggregateType(String aggregateType, Metadata metadata)
        {
            return new Builder(metadata).aggregateType(aggregateType).build();
        }

        public static DomainEventMetadata aggregate(String aggregateType, String aggregateId, Metadata metadata)
        {
            return new Builder(metadata)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .build();
        }

        public Builder(Metadata metadata) {
            this(metadata.toBuilder());
        }

        public Builder domain(String value)
        {
            builder.add("domain", value);
            return this;
        }

        public Builder aggregateId(String value)
        {
            builder.add("aggregateId", value);
            return this;
        }

        public Builder aggregateType(String name)
        {
            builder.add("aggregateType", name);
            return this;
        }

        public Builder commandType(Class<? extends Command> commandClass)
        {
            builder.add("commandType", commandClass.getName());
            return this;
        }

        public DomainEventMetadata build()
        {
            return new DomainEventMetadata(builder.build());
        }
    }

    public String getDomain()
    {
        return context.getString("domain").orElse("default");
    }

    public String getAggregateType()
    {
        return context.getString("aggregateType").orElseThrow();
    }

    public String getAggregateId()
    {
        return context.getString("aggregateId").orElseThrow();
    }

    public String getCommandType()
    {
        return context.getString("commandType").orElseThrow();
    }

}
