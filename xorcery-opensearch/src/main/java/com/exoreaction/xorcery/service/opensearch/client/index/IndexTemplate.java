package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record IndexTemplate(ObjectNode json)
    implements JsonElement
{

    public String name() {
        return getString("name").orElseThrow();
    }
}
