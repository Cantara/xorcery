package com.exoreaction.reactiveservices.service.domainevents.api;

import com.exoreaction.reactiveservices.disruptor.Metadata;

public record DomainEventMetadata(Metadata metadata)
{
    public String domain()
    {
        return metadata.getString("domain").orElse("default");
    }

    public String aggregateType()
    {
        return metadata.getString("aggregateType").orElseThrow();
    }

    public String commandType()
    {
        return metadata.getString("commandType").orElseThrow();
    }
}
