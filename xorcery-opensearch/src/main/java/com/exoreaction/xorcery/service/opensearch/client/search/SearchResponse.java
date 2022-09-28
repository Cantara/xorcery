package com.exoreaction.xorcery.service.opensearch.client.search;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.service.opensearch.client.index.IndexTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public record SearchResponse(ObjectNode json)
        implements JsonElement {

    public Hits hits()
    {
        return getObjectAs("hits", Hits::new).orElseThrow();
    }
}
