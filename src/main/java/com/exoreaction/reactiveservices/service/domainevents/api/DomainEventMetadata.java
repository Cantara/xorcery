package com.exoreaction.reactiveservices.service.domainevents.api;

import com.exoreaction.reactiveservices.disruptor.StandardMetadata;

public interface DomainEventMetadata
    extends StandardMetadata
{
    String DOMAIN = "d";
    String AGGREGATE_TYPE ="at";
    String COMMAND_TYPE = "ct";

    String POSITION = "pos";
}
