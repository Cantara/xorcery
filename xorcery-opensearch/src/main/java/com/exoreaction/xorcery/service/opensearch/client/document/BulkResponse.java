package com.exoreaction.xorcery.service.opensearch.client.document;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record BulkResponse(ObjectNode json)
    implements JsonElement
{
    public boolean hasErrors()
    {
        return getBoolean("errors").orElse(false);
    }
}
