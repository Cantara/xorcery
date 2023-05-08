package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.dns.client.discovery.DnsDiscoveryService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({DnsLookup.class})
@Rank(2)
public class DiscoveryLookupHK2
    extends com.exoreaction.xorcery.service.dns.client.DiscoveryLookup
{
    @Inject
    public DiscoveryLookupHK2(Provider<DnsDiscoveryService> discoveryService) {
        super(discoveryService::get);
    }
}
