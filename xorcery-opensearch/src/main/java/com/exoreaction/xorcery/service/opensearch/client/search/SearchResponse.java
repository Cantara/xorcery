package com.exoreaction.xorcery.service.opensearch.client.search;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SearchResponse(ObjectNode json)
        implements JsonElement {

    public Hits hits()
    {
        return getObjectAs("hits", Hits::new).orElseThrow();
    }
}
