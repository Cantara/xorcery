package com.exoreaction.xorcery.service.dns.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

import java.util.List;
import java.util.Optional;

public record DnsServerConfiguration(Configuration context)
        implements ServiceConfiguration {
    public int getPort() {
        return context.getInteger("port").orElse(53);
    }

    public Optional<List<KeyConfiguration>> getKeys() {
        return context.getObjectListAs("keys", KeyConfiguration::new);
    }

    public Optional<List<ZoneConfiguration>> getZones() {
        return context.getObjectListAs("dns.server.zones", ZoneConfiguration::new);
    }
}
