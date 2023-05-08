package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service(name = "dns.client")
@ContractsProvided({com.exoreaction.xorcery.service.dns.client.DnsLookupService.class})
public class DnsLookupServiceHK2 extends com.exoreaction.xorcery.service.dns.client.DnsLookupService {

    @Inject
    public DnsLookupServiceHK2(IterableProvider<DnsLookup> dnsLookups) {
        super(dnsLookups);
    }
}
