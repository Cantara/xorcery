package com.exoreaction.xorcery.eventstore.reactor;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record EventStoreCommit(ObjectNode json)
    implements JsonElement
{
}
