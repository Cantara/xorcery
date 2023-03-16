package com.exoreaction.xorcery.service.dns.server;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AllowUpdate(ObjectNode json)
        implements JsonElement {
    public String getKey() {
        return getString("key").orElseThrow(() -> new IllegalArgumentException("No key name set"));
    }
}
