package com.exoreaction.xorcery.eventstore.client;

public record EventStoreCommit(String stream, String lastEventId, long position)
{
}
