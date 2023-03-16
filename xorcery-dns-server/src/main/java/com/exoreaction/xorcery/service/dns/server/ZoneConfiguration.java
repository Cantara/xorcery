package com.exoreaction.xorcery.service.dns.server;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Optional;

public record ZoneConfiguration(ObjectNode json)
        implements JsonElement {
    public String getName() {
        return getString("name").orElseThrow(() -> new IllegalArgumentException("No zone name set"));
    }

    public Optional<List<AllowUpdate>> getAllowUpdate() {
        return getObjectListAs("allow-update", AllowUpdate::new);
    }
}
