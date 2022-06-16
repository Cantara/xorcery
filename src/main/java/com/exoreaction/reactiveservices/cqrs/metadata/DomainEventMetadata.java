package com.exoreaction.reactiveservices.cqrs.metadata;

import com.exoreaction.reactiveservices.cqrs.aggregate.Aggregate;
import com.exoreaction.reactiveservices.cqrs.aggregate.Command;
import com.exoreaction.reactiveservices.jsonapi.model.Meta;

public record DomainEventMetadata(Metadata metadata)
{
    public record Builder(Metadata.Builder builder)
    {
        public static Metadata aggregateId(String aggregateId, Metadata metadata)
        {
            return new Builder(metadata).aggregateId(aggregateId).build().metadata();
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

        public Builder aggregateType(Class<? extends Aggregate> aggregateClass)
        {
            builder.add("aggregateType", aggregateClass.getName());
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
        return metadata.getString("domain").orElse("default");
    }

    public String getAggregateType()
    {
        return metadata.getString("aggregateType").orElseThrow();
    }

    public String getAggregateId()
    {
        return metadata.getString("aggregateId").orElseThrow();
    }

    public String getCommandType()
    {
        return metadata.getString("commandType").orElseThrow();
    }

}
