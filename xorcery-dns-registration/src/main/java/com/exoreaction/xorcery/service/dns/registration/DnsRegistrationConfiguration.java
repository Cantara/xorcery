package com.exoreaction.xorcery.service.dns.registration;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

import java.time.Duration;
import java.util.Optional;

public record DnsRegistrationConfiguration(Configuration context)
        implements ServiceConfiguration {
    public Duration getTimeout() {
        return Duration.parse("PT" + context().getString("timeout").orElse("30s"));
    }

    public Duration getTtl() {
        return Duration.parse("PT" + context().getString("ttl").orElse("60s"));
    }

    public Optional<DnsKeyConfiguration> getKey() {
        return context.getObjectAs("key", Configuration::new).map(DnsKeyConfiguration::new);
    }
}
