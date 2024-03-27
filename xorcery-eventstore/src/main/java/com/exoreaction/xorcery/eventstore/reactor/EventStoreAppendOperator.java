package com.exoreaction.xorcery.eventstore.reactor;

import reactor.core.publisher.SynchronousSink;

import java.util.List;
import java.util.function.Function;

public class EventStoreAppendOperator
    implements Function<List<MetadataByteBuffer>, SynchronousSink<EventStoreCommit>>
{
    @Override
    public SynchronousSink<EventStoreCommit> apply(List<MetadataByteBuffer> metadataByteBuffers) {

/*
        for (MetadataByteBuffer metadataByteBuffer : metadataByteBuffers) {
            EventsMetadata emd = new EventsMetadata(event.metadata());
            UUID eventId = metadataByteBuffer.metadata().getString(DomainEventMetadata."correlationId")Domaiemd.getCorrelationId().map(UUID::fromString).orElseGet(UUID::randomUUID);
            String eventType = emd.commandName();

            EventData eventData = EventDataBuilder.json(eventId, eventType, event.event().array())
                    .metadataAsBytes(metadataMapper.writeValueAsBytes(event.metadata().metadata())).build();
            eventBatch.add(eventData);

        }
*/

        return null;
    }
}
