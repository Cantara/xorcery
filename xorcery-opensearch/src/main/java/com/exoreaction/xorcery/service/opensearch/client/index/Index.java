package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record Index(ObjectNode json)
    implements JsonElement
{
    public Index(JsonNode json) {
        this((ObjectNode)json);
    }
}
