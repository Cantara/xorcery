package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AcknowledgedResponse(ObjectNode json)
    implements JsonElement
{
    public boolean isAcknowledged() {
        return getBoolean("acknowledged").orElse(false);
    }
}
