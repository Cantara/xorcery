package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URL;
import java.util.Optional;

public record JettyServerHttp2Configuration(Configuration configuration) {

    public boolean isEnabled()
    {
        return configuration.getBoolean("enabled").orElse(false);
    }
}
