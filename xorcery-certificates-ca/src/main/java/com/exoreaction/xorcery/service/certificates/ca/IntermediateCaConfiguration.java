package com.exoreaction.xorcery.service.certificates.ca;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

import java.time.Duration;

public record IntermediateCaConfiguration(Configuration context)
        implements ServiceConfiguration {
    public String getAlias() {
        return context().getString("alias").orElse("intermediate");
    }

    public Duration getValidity() {
        return Duration.parse("P" + context().getString("validity").orElse("90d"));
    }
}
