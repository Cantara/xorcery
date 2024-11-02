package dev.xorcery.dns.update.route53;

import dev.xorcery.configuration.Configuration;

import static dev.xorcery.configuration.Configuration.missing;

public record Route53DnsUpdateConfiguration(Configuration configuration) {

    public static Route53DnsUpdateConfiguration get(Configuration configuration) {
        return new Route53DnsUpdateConfiguration(configuration.getConfiguration("dns.route53"));
    }

    public String getRegion()
    {
        return configuration.getString("region").orElseThrow(missing("region"));
    }
}
