package com.exoreaction.xorcery.eventstore.client.api;

public record EventStoreCommit(String streamId, long streamPosition, String lastEventId)
{
}
