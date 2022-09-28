package com.exoreaction.xorcery.service.opensearch.client.search;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;

public record Hits(ObjectNode json)
        implements JsonElement {
    public List<Document> documents() {
        return getObjectListAs("hits", Document::new)
                .orElse(Collections.emptyList());
    }
}
