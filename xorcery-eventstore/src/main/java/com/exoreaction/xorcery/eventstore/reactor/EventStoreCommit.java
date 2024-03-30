package com.exoreaction.xorcery.eventstore.reactor;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record EventStoreCommit(String stream, String lastEventId, long position)
{
}
