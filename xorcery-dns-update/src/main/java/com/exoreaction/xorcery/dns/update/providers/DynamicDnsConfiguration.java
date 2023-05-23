package com.exoreaction.xorcery.dns.update.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;

import java.time.Duration;
import java.util.Optional;

public record DynamicDnsConfiguration(Configuration context)
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
