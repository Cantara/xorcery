package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record JettyClientSslConfiguration(Configuration configuration) {

    public boolean isEnabled() {
        return configuration.getBoolean("enabled").orElse(false);
    }

    public String getAlias() {
        return configuration.getString("alias").orElse("self");
    }

    public String getEndpointIdentificationAlgorithm() {
        return configuration.getString("endpointIdentificationAlgorithm").orElse("HTTPS");
    }

    public boolean isTrustAll() {
        return configuration.getBoolean("trustAll").orElse(false);
    }
}
