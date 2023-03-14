package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

import java.time.Duration;

public record JettyClientConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public Duration getIdleTimeout() {
        return Duration.parse("PT" + context().getString("idleTimeout").orElse("-1s"));
    }

}
