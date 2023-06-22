package com.exoreaction.xorcery.jwt.server;

import com.exoreaction.xorcery.configuration.Configuration;

public record JwtServerConfiguration(Configuration configuration) {

    public String getIssuer()
    {
        return configuration.getString("issuer").orElseThrow(Configuration.missing("issuer"));
    }
}
