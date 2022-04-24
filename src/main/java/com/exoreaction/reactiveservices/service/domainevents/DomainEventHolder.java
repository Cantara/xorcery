package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.disruptor.EventHolderWithResult;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.domainevents.api.Metadata;

import java.util.List;

public class DomainEventHolder
    extends EventHolderWithResult<DomainEvents, Metadata>
{
}
