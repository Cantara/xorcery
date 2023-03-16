package com.exoreaction.xorcery.service.dns.server;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record KeyConfiguration(ObjectNode json)
        implements JsonElement {
    public String getName() {
        return getString("name").orElseThrow(() -> new IllegalArgumentException("No key name set"));
    }

    public String getSecret() {
        return getString("secret").orElseThrow(() -> new IllegalArgumentException("No key secret set"));
    }

    public String getAlgorithm() {
        return getString("algorithm").orElse("hmac-md5");
    }
}
