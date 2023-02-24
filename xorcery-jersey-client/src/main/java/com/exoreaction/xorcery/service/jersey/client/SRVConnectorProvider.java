package com.exoreaction.xorcery.service.jersey.client;

import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Configuration;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

public class SRVConnectorProvider
        implements ConnectorProvider {
    private final DnsLookup dnsLookup;
    private String scheme;
    private ConnectorProvider delegateProvider;

    public SRVConnectorProvider(DnsLookupService dnsLookup, String scheme, ConnectorProvider delegateProvider) {
        this.dnsLookup = dnsLookup;
        this.scheme = scheme;
        this.delegateProvider = delegateProvider;
    }

    @Override
    public Connector getConnector(Client client, Configuration runtimeConfig) {
        return new SRVConnector(dnsLookup, scheme, delegateProvider.getConnector(client, runtimeConfig));
    }
}
