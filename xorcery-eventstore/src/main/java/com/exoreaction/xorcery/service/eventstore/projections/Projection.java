package com.exoreaction.xorcery.service.eventstore.projections;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record Projection(ObjectNode json)
        implements JsonElement {
    String getName() {
        return getString("name").orElseThrow();
    }

    String getQuery() {
        return getString("query").orElseThrow();
    }

    public boolean isEmitEnabled() {
        return getBoolean("emitenabled").orElse(false);
    }
}
