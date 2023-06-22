package com.exoreaction.xorcery.jetty.server.security.jwt;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.Map;

public record JwtConfiguration(Configuration configuration) {

    public Map<String,IssuerConfiguration> getIssuers()
    {
        return configuration.getObjectAs("issuers",JsonElement.toMap(json -> new IssuerConfiguration(new Configuration((ObjectNode)json))) ).orElse(Collections.emptyMap());
    }
}
