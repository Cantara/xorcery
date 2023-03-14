package com.exoreaction.xorcery.service.certificates.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

import java.time.Duration;

public record CertificatesServerConfiguration(Configuration context)
        implements ServiceConfiguration {
    public String getAlias() {
        return context().getString("alias").orElse("intermediate");
    }

    public Duration getValidity() {
        return Duration.parse("P" + context().getString("validity").orElse("90d"));
    }
}
