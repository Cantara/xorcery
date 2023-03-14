package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URL;
import java.util.Optional;

public record JettyServerSslConfiguration(Configuration configuration) {
    public Optional<URL> getCRLs() {
        return configuration.getResourceURL("crls");
    }

    public String getAlias() {
        return configuration.getString("alias").orElse("self");
    }

    public boolean isTrustAll() {
        return configuration.getBoolean("trustAll").orElse(false);
    }

    public boolean isNeedClientAuth() {
        return configuration.getBoolean("needClientAuth").orElse(false);
    }

    public boolean isWantClientAuth() {
        return configuration.getBoolean("wantClientAuth").orElse(false);
    }
}
