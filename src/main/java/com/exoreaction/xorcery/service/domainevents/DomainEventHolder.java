package com.exoreaction.xorcery.service.domainevents;

import com.exoreaction.xorcery.cqrs.aggregate.DomainEvents;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;

public class DomainEventHolder
    extends WithResult<WithMetadata<DomainEvents>, Metadata>
{
}
